package com.example.alanflores.clientebluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> dispositivos;
    public BluetoothSocket bluetoothSocket;
    private PrintWriter printWriter;
    private EditText editMesaje;
    private Button buttonEnviar;
    private ListView listView;
    private static final int PETICION_BLUETOOTH = 0;
    private final UUID MI_UUID = UUID.fromString(getResources().getString(R.string.UUID));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editMesaje = (EditText) findViewById(R.id.edit_mesaje);
        buttonEnviar = (Button) findViewById(R.id.button_enviar);
        listView = (ListView) findViewById(R.id.listView);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            Toast.makeText(MainActivity.this, "No se ha podido encontrar un dispositivo Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }else {
            if(bluetoothAdapter.isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, PETICION_BLUETOOTH);
            }
        }

        dispositivos = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(dispositivos);

        listView.setOnItemClickListener(itemClickListener);
        buttonEnviar.setOnClickListener(clickListener);
        buttonEnviar.setEnabled(false);
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        private BluetoothDevice bluetoothDevice;

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            bluetoothAdapter.cancelDiscovery();
            String nombreBluetooth = ((TextView)view).getText().toString();
            String direccionBluetooth = nombreBluetooth.substring(nombreBluetooth.length() - 17);

            bluetoothDevice = bluetoothAdapter.getRemoteDevice(direccionBluetooth);
            Toast.makeText(MainActivity.this, "Realizar conexion", Toast.LENGTH_SHORT).show();
            ConexionClienteAsync conexionClienteAsync = new ConexionClienteAsync();
            conexionClienteAsync.execute(bluetoothDevice);

        }
    };

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            enviarMensaje();
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accion = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(accion)){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                    dispositivos.add(bluetoothDevice.getName() + bluetoothDevice.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(accion)){
                Toast.makeText(MainActivity.this, "Busqueda de dispositivos terminas", Toast.LENGTH_SHORT).show();
                if(dispositivos.getCount() == 0){
                    dispositivos.add("No se encontraron dispositivos");
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.actualizar){
            iniciarDescubrimiento();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case PETICION_BLUETOOTH:
                if (resultCode == RESULT_OK){
                    Toast.makeText(MainActivity.this, "Bluetooth habilitado ...", Toast.LENGTH_SHORT).show();
                    iniciarDescubrimiento();
                }else if (resultCode == RESULT_CANCELED){
                    Toast.makeText(MainActivity.this, "Bluetooth deshabilitado", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bluetoothAdapter != null){
            bluetoothAdapter.cancelDiscovery();
            this.unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bluetoothAdapter == null){
            iniciarDescubrimiento();
            this.registrarBroadcast();
        }
    }

    private void registrarBroadcast(){
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void iniciarDescubrimiento(){
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        Toast.makeText(MainActivity.this, "Iniciando descubrimiento de sispositivos", Toast.LENGTH_SHORT).show();
        bluetoothAdapter.startDiscovery();
    }

    private void enviarMensaje(){
        if(printWriter != null){
            try{
                String mensaje = editMesaje.getText().toString();
                printWriter.print(mensaje);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else
            Toast.makeText(MainActivity.this, "Conexion no establecida con dispositivo", Toast.LENGTH_SHORT).show();
    }

    class ConexionClienteAsync extends AsyncTask<BluetoothDevice,String,Boolean>{

        BluetoothDevice bluetoothDevice;
        BluetoothSocket bluetoothSocketTemp;
        @Override
        protected Boolean doInBackground(BluetoothDevice... bluetoothDevices) {
            this.bluetoothDevice = bluetoothDevices[0];
            try{
                bluetoothSocketTemp = bluetoothDevice.createRfcommSocketToServiceRecord(MI_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            bluetoothSocket = bluetoothSocketTemp;
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }



        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean){
                Toast.makeText(MainActivity.this, "Conxion establecida", Toast.LENGTH_SHORT).show();
                buttonEnviar.setEnabled(true);
            }else {
                Toast.makeText(MainActivity.this, "Error al intentar conectar", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
