package com.example.multigrafo



import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.multigrafo.databinding.ActivityMainBinding
import com.ingenieriajhr.blujhr.BluJhr
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries




class MainActivity : AppCompatActivity() {

    //bluetooth var
    lateinit var blue: BluJhr
    var devicesBluetooth = ArrayList<String>()

    //visible ListView
    var graphviewVisible = true

    //graphviewSeries
    lateinit var modulo: LineGraphSeries<DataPoint?>

    //nos indica si estamos recibiendo datos o no
    var initGraph = false
    //nos almacena el estado actual de la conexion bluetooth
    var stateConn = BluJhr.Connected.False

    //valor que se suma al eje x despues de cada actualizacion
    var ejeX = 0.6

    //sweet alert necesarios
    lateinit var loadSweet : SweetAlertDialog
    lateinit var errorSweet : SweetAlertDialog
    lateinit var okSweet : SweetAlertDialog
    lateinit var disconnection : SweetAlertDialog

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_main)
        val view = binding.root
        setContentView(view)

        //init var sweetAlert
        initSweet()

        blue = BluJhr(this)
        blue.onBluetooth()

        binding.btnViewDevice.setOnClickListener {
            when (graphviewVisible) {
                false -> invisibleListDevice()
                true -> visibleListDevice()
            }
        }

        binding.listDeviceBluetooth.setOnItemClickListener { adapterView, view, i, l ->
            if (devicesBluetooth.isNotEmpty()) {
                blue.connect(devicesBluetooth[i])
                //genera error si no se vuelve a iniciar los objetos sweet
                initSweet()
                blue.setDataLoadFinishedListener(object : BluJhr.ConnectedBluetooth {
                    override fun onConnectState(state: BluJhr.Connected) {
                        stateConn = state
                        when (state) {
                            BluJhr.Connected.True -> {
                                loadSweet.dismiss()
                                okSweet.show()
                                invisibleListDevice()
                                rxReceived()
                            }

                            BluJhr.Connected.Pending -> {
                                loadSweet.show()
                            }

                            BluJhr.Connected.False -> {
                                loadSweet.dismiss()
                                errorSweet.show()
                            }

                            BluJhr.Connected.Disconnect -> {
                                loadSweet.dismiss()
                                disconnection.show()
                                visibleListDevice()
                            }

                        }
                    }
                })
            }
        }

        //graphview
        initGraph()

        /*
        binding.btnInitStop.setOnClickListener {
            if (stateConn == BluJhr.Connected.True){
                initGraph = when(initGraph){
                    true->{
                        blue.bluTx("0")
                        binding.btnInitStop.text = "START"
                        false
                    }
                    false->{
                        blue.bluTx("1")
                        binding.btnInitStop.text = "STOP"
                        true
                    }
                }
            }
        }
        */

    }

    private fun initSweet() {
        loadSweet = SweetAlertDialog(this,SweetAlertDialog.PROGRESS_TYPE)
        okSweet = SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
        errorSweet = SweetAlertDialog(this,SweetAlertDialog.ERROR_TYPE)
        disconnection = SweetAlertDialog(this,SweetAlertDialog.NORMAL_TYPE)

        loadSweet.titleText = "Conectando"
        loadSweet.setCancelable(false)
        errorSweet.titleText = "Algo salio mal"

        okSweet.titleText = "Conectado"
        disconnection.titleText = "Desconectado"
    }


    private fun initGraph() {
        //permitime controlar los ejes manualmente
        binding.graph.viewport.isXAxisBoundsManual = true;
        binding.graph.viewport.setMinX(0.0);
        binding.graph.viewport.setMaxX(10.0);
        binding.graph.viewport.setMaxY(1024.0)
        binding.graph.viewport.setMinY(0.0)

        //permite realizar zoom y ajustar posicion eje x
        binding.graph.viewport.isScalable = true
        binding.graph.viewport.setScalableY(false)

        modulo = LineGraphSeries()
        //draw points
        modulo.isDrawDataPoints = true;
        //draw below points
        modulo.isDrawBackground = true;
        //color series
        modulo.color = Color.GREEN

        binding.graph.addSeries(modulo);


    }

    private fun rxReceived() {
        blue.loadDateRx(object:BluJhr.ReceivedData{
            override fun rxDate(rx: String) {
                ejeX+=0.6
                System.out.println("AQUI");
                if (rx.contains("Electro")){
                    binding.txtModo.text = "Modo:\n $rx"
                }
                if (rx.equals("Nuevo")){
                    binding.txtModo.text = "Esperando ..."
                }
                if(rx.equals("Comenzando")){
                    binding.txtModo.text = "Comenzando..."
                }
                if(rx.equals("Saliendo y guardando")){
                    binding.txtModo.text = rx
                }
                else{
                    modulo.appendData(DataPoint(ejeX, rx.toDouble()), true, 22)
                    binding.txtPot.text = "Value: $rx"
                }

                /*
                if (rx.contains("t")){
                    val date = rx.replace("t","")
                    modulo.appendData(DataPoint(ejeX, date.toDouble()), true, 22)
                }else{
                    if (rx.contains("p")){
                        val date = rx.replace("p","")
                        binding.txtPot.text = "Value: $date"
                    }
                }
                */
            }
        })
    }


    /**
     * invisible listDevice
     */
    private fun invisibleListDevice() {
        binding.containerGraph.visibility = View.VISIBLE
        binding.containerDevice.visibility = View.GONE
        graphviewVisible = true
        binding.btnViewDevice.text = "DEVICE"
    }

    /**
     * visible list device
     */
    private fun visibleListDevice() {
        binding.containerGraph.visibility = View.GONE
        binding.containerDevice.visibility = View.VISIBLE
        graphviewVisible = false
        binding.btnViewDevice.text = "GraphView"

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!blue.stateBluetoooth() && requestCode == 100){
            blue.initializeBluetooth()
        }else{
            if (requestCode == 100){
                devicesBluetooth = blue.deviceBluetooth()
                if (devicesBluetooth.isNotEmpty()){
                    val adapter = ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,devicesBluetooth)
                    binding.listDeviceBluetooth.adapter = adapter
                }else{
                    Toast.makeText(this, "No tienes vinculados dispositivos", Toast.LENGTH_SHORT).show()
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (blue.checkPermissions(requestCode,grantResults)){
            Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show()
            blue.initializeBluetooth()
        }else{
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
                blue.initializeBluetooth()
            }else{
                Toast.makeText(this, "Algo salio mal", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
