package com.ebr.mifareutility;

import java.io.IOException;
import java.util.Locale;

import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.nfc.Tag;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.Toast;


public class MainActivity extends Activity {


    private static final String TAG = "nfcinventory_simple";

    // NFC-related variables
    NfcAdapter mNfcAdapter;
    PendingIntent mNfcPendingIntent;
    IntentFilter[] mReadWriteTagFilters;
    private boolean mWriteMode = false;
    private boolean mAuthenticationMode = false;
    private boolean ReadUIDMode = true;
    String[][]mTechList;

    // UI elements
    EditText mTagUID;
    EditText mCardType;
    EditText mHexKeyA;
    EditText mHexKeyB;

    EditText mAuthSector;
    EditText mIOSector;
    EditText mAccessSector;

    EditText mBloque;
    EditText mDataBloque;
    EditText mDatatoWrite;
    AlertDialog mTagDialog;
    RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec spec1=tabHost.newTabSpec("T1");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Auth");

        TabHost.TabSpec spec2=tabHost.newTabSpec("T2");
        spec2.setContent(R.id.tab2);
        spec2.setIndicator("Read/Write");

        TabHost.TabSpec spec4=tabHost.newTabSpec("T4");
        spec4.setContent(R.id.tab4);
        spec4.setIndicator("Access");

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec4);

        /*
        mTagUID = ((EditText) findViewById(R.id.tag_uid));
        mCardType = ((EditText) findViewById(R.id.cardtype));
        mDataBloque = ((EditText) findViewById(R.id.editTextBloqueLeido));
        mDatatoWrite = ((EditText) findViewById(R.id.editTextBloqueAEscribir));

        */

        //Keys used fot all operations
        mHexKeyA = ((EditText) findViewById(R.id.editTextKeyA));
        mHexKeyB = ((EditText) findViewById(R.id.editTextKeyB));

        //Sector is needed on the three tabs
        mAuthSector = ((EditText) findViewById(R.id.editTextAuthSector));
        mIOSector = ((EditText) findViewById(R.id.editTextIOSector));
        mAccessSector = ((EditText) findViewById(R.id.editTextAccessSector));

        mBloque = ((EditText) findViewById(R.id.editTextBlock));
        mRadioGroup = ((RadioGroup) findViewById(R.id.keySelectorRadioGroup));


        //Click listeners for auth, read and write
        findViewById(R.id.authWithSelectedKey).setOnClickListener(mTagAuthenticate);
        findViewById(R.id.readButton).setOnClickListener(mTagRead);
        findViewById(R.id.writeButton).setOnClickListener(mTagWrite);


        //Get a reference to the NFC adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // if null, this is not a NFC powered device
        if (mNfcAdapter == null)
        {
            Toast.makeText(this,
                    "Su dispositivo no soporta NFC. No se puede correr la aplicación.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // check if NFC is enabled, if not, open settings to activate
        checkNfcEnabled();


        // Handle foreground NFC scanning in this activity by creating a
        // PendingIntent with FLAG_ACTIVITY_SINGLE_TOP flag so each new scan
        // is not added to the Back Stack
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Create intent filter to handle MIFARE NFC tags detected from inside our
        // application when in "read mode":
        IntentFilter mifareDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        //Add our custom MIME type
        try {
            mifareDetected.addDataType("application/com.ebr.mifareutility");
        } catch (MalformedMimeTypeException e)
        {
            throw new RuntimeException("No se pudo añadir un tipo MIME.", e);
        }

        // Create intent filter to detect any MIFARE NFC tag when attempting to write
        // to a tag in "write mode"
        //IntentFilter tagDetected = new IntentFilter(
        //         NfcAdapter.ACTION_TAG_DISCOVERED);

        // create IntentFilter arrays:
        //mWriteTagFilters = new IntentFilter[] { tagDetected };
        mReadWriteTagFilters = new IntentFilter[] { mifareDetected };


        // Setup a tech list for all NfcF tags
        mTechList = new String[][] { new String[] { MifareClassic.class.getName() } };

        resolveReadIntent(getIntent());

    }


    //Read intent
    void resolveReadIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            if (ReadUIDMode)
            {
                String tipotag = "";
                String tamano = "";
                byte[] tagUID = tagFromIntent.getId();
                String hexUID = getHexString(tagUID, tagUID.length);
                Log.i(TAG, "Tag UID: " + hexUID);

                Editable UIDField = mTagUID.getText();
                UIDField.clear();
                UIDField.append(hexUID);

                switch(mfc.getType())
                {
                    case 0: tipotag = "Mifare Classic"; break;
                    case 1: tipotag = "Mifare Plus"; break;
                    case 2: tipotag = "Mifare Pro"; break;
                    default: tipotag = "Mifare Desconocido"; break;
                }

                switch(mfc.getSize())
                {
                    case 1024: tamano = " (1K Bytes)"; break;
                    case 2048: tamano = " (2K Bytes)"; break;
                    case 4096: tamano = " (4K Bytes)"; break;
                    case 320: tamano = " (MINI - 320 Bytes)"; break;
                    default: tamano = " (Tamaño desconocido)"; break;
                }

                Log.i(TAG, "Card Type: " + tipotag + tamano);

                Editable CardtypeField = mCardType.getText();
                CardtypeField.clear();
                CardtypeField.append(tipotag + tamano);

            } else
            {
                try {
                    mfc.connect();
                    boolean auth = false;
                    String hexkey = "";
                    int id = mRadioGroup.getCheckedRadioButtonId();

                    int sector = mfc.blockToSector(Integer.valueOf(mBloque.getText().toString()));
                    byte[] datakey;

                    /*

                    if (id == R.id.radioButtonKeyA){
                        hexkey = mHexKeyA.getText().toString();
                        datakey = hexStringToByteArray(hexkey);
                        auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                    }
                    else if (id == R.id.radioButtonKeyB){
                        hexkey = mHexKeyB.getText().toString();
                        datakey = hexStringToByteArray(hexkey);
                        auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                    }
                    else {
                        //no item selected poner toast
                        Toast.makeText(this,
                                "°Seleccionar llave A o B!",
                                Toast.LENGTH_LONG).show();
                        mfc.close();
                        return;
                    }


                    */
                    if(auth){
                        int bloque = Integer.valueOf(mBloque.getText().toString());
                        byte[] dataread = mfc.readBlock(bloque);

                        String blockread = getHexString(dataread, dataread.length);
                        Log.i(TAG, "Bloque Leido: " + blockread);

                        Editable BlockField = mDataBloque.getText();
                        BlockField.clear();
                        BlockField.append(blockread);

                        Toast.makeText(this,
                                "Lectura de bloque EXITOSA.",
                                Toast.LENGTH_LONG).show();


                    }else{ // Authentication failed - Handle it
                        Editable BlockField = mDataBloque.getText();
                        BlockField.clear();
                        Toast.makeText(this,
                                "Lectura de bloque FALLIDA dado autentificación fallida.",
                                Toast.LENGTH_LONG).show();
                    }

                    mfc.close();
                    mTagDialog.cancel();

                }catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }

        }
    }


    //Write intent
    void resolveWriteIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            try {
                mfc.connect();
                boolean auth = false;
                String hexkey = "";
                int id = mRadioGroup.getCheckedRadioButtonId();
                int bloque = Integer.valueOf(mBloque.getText().toString());
                int sector = mfc.blockToSector(bloque);
                byte[] datakey;

                /*

                if (id == R.id.radioButtonKeyA){
                    hexkey = mHexKeyA.getText().toString();
                    datakey = hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }
                else if (id == R.id.radioButtonKeyB){
                    hexkey = mHexKeyB.getText().toString();
                    datakey = hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }
                else {
                    //no item selected poner toast
                    Toast.makeText(this,
                            "°Seleccionar llave A o B!",
                            Toast.LENGTH_LONG).show();
                    mfc.close();
                    return;
                }

                */

                if(auth){
                    String strdata = mDatatoWrite.getText().toString();
                    byte[] datatowrite = hexStringToByteArray(strdata);
                    mfc.writeBlock(bloque, datatowrite);

                    Toast.makeText(this,
                            "Escritura a bloque EXITOSA.",
                            Toast.LENGTH_LONG).show();


                }else{ // Authentication failed - Handle it
                    Toast.makeText(this,
                            "Escritura a bloque FALLIDA dado autentificación fallida.",
                            Toast.LENGTH_LONG).show();
                }

                mfc.close();
                mTagDialog.cancel();

            }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

        }
    }

    //Auth intent
    void resolveAuthIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            try {
                mfc.connect();
                boolean auth = false;
                String hexkey = "";
                int id = mRadioGroup.getCheckedRadioButtonId();
                int sector = Integer.valueOf(mAuthSector.getText().toString());
                byte[] datakey;
/*
                if (id == R.id.radioButtonKeyA){
                    hexkey = mHexKeyA.getText().toString();
                    datakey = hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }
                else if (id == R.id.radioButtonKeyB){
                    hexkey = mHexKeyB.getText().toString();
                    datakey = hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }
                else {
                    //no item selected poner toast
                    Toast.makeText(this,
                            "°Seleccionar llave A o B!",
                            Toast.LENGTH_LONG).show();
                    mfc.close();
                    return;
                }
*/
                if(auth){
                    Toast.makeText(this,
                            "Autentificación de sector EXITOSA.",
                            Toast.LENGTH_LONG).show();
                }else{ // Authentication failed - Handle it
                    Toast.makeText(this,
                            "Autentificación de sector FALLIDA.",
                            Toast.LENGTH_LONG).show();
                }
                mfc.close();
            }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }




    /* Called when the activity will start interacting with the user. */
    @Override
    public void onResume()
    {
        super.onResume();

        // Double check if NFC is enabled
        checkNfcEnabled();

        Log.d(TAG, "onResume: " + getIntent());

        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mReadWriteTagFilters, mTechList);
    }


    /*
     * This is called for activities that set launchMode to "singleTop" or
     * "singleTask" in their manifest package, or if a client used the
     * FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
     */
    @Override
    public void onNewIntent(Intent intent)
    {
        Log.d(TAG, "onNewIntent: " + intent);
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);

        if (mAuthenticationMode)
        {
            // Currently in tag AUTHENTICATION mode
            resolveAuthIntent(intent);
            mTagDialog.cancel();
        }
        else if (!mWriteMode)
        {
            // Currently in tag READING mode
            resolveReadIntent(intent);
        } else
        {
            // Currently in tag WRITING mode
            resolveWriteIntent(intent);
        }
    }


    /* Called when the system is about to start resuming a previous activity. */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause: " + getIntent());
        mNfcAdapter.disableForegroundDispatch(this);

    }


    private void enableTagWriteMode()
    {
        mWriteMode = true;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableTagReadMode()
    {
        mWriteMode = false;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableTagAuthMode()
    {
        mAuthenticationMode = true;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }


    /*
     * **** TAG AUTHENTICATE METHODS ****
     */
    private View.OnClickListener mTagAuthenticate = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            //Set auth flag to true
            enableTagAuthMode();

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_authenticate))
                    .setMessage(getString(R.string.ready_to_authenticate_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    dialog.cancel();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            mAuthenticationMode = false;
                        }
                    });
            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };


    /*
     * **** TAG READ METHODS ****
     */

    private View.OnClickListener mTagRead = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            enableTagReadMode();
            ReadUIDMode = false;

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_read))
                    .setMessage(getString(R.string.ready_to_read_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    dialog.cancel();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            enableTagReadMode();
                            ReadUIDMode = true;
                        }
                    });
            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };


    /*
     * **** TAG WRITE METHODS ****
     */

    private View.OnClickListener mTagWrite = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            enableTagWriteMode();

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(getString(R.string.ready_to_write_instructions))
                    .setCancelable(true)
                    .setNegativeButton("Cancelar",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    dialog.cancel();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            enableTagReadMode();
                        }
                    });
            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };



    /*
     * **** HELPER METHODS ****
     */

    private void checkNfcEnabled()
    {
        Boolean nfcEnabled = mNfcAdapter.isEnabled();
        if (!nfcEnabled)
        {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.warning_nfc_is_off))
                    .setMessage(getString(R.string.turn_on_nfc))
                    .setCancelable(false)
                    .setPositiveButton("Actualizar Settings",
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    startActivity(new Intent(
                                            android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            }).create().show();
        }
    }

    public static String getHexString(byte[] b, int length)
    {
        String result = "";
        Locale loc = Locale.getDefault();

        for (int i = 0; i < length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += " ";
        }
        return result.toUpperCase(loc);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }


    /*
     * **** MENU OPTIONS ****
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.menu_about){
            Toast.makeText(this, "You clicked on About Item",
                    Toast.LENGTH_LONG).show();
            return true;
        }else if (item.getItemId() == R.id.menu_ayuda){
            Toast.makeText(this, "You clicked on Ayuda Item",
                    Toast.LENGTH_LONG).show();
            return true;

        }else if (item.getItemId() == R.id.menu_settings){
            Toast.makeText(this, "You clicked on Settings Item",
                    Toast.LENGTH_LONG).show();
            return true;
        }else {
            return false;
        }
    }


}

