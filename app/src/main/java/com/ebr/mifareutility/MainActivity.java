package com.ebr.mifareutility;


import java.io.IOException;
import java.util.Arrays;

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
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.Toast;

public class MainActivity extends Activity {


    private static final String TAG = "Mifare";

    // NFC-related variables
    NfcAdapter mNfcAdapter;
    PendingIntent mNfcPendingIntent;
    IntentFilter[] mReadWriteTagFilters;
    String[][]mTechList;

    //Enum type for modes
    public enum Mode {
        INFOMODE,
        AUTHMODE,
        READMODE,
        WRITEMODE,
        READACCESSMODE,
        WRITEACCESSMODE,
        READVALUEMODE,
        WRITEVALUEMODE,
        INCREMENTVALUEMODE,
        DECREMENTVALUEMODE,
    }

    //Mode variable
    private Mode  currentMode = Mode.INFOMODE;
    private boolean ReadUIDMode = true;

    // UI elements on AUTH TAB
    EditText mAuthKeyA;
    EditText mAuthKeyB;
    EditText mAuthSector;
    RadioGroup mAuthRadioGroup;

    //UI elements on READ/WRITE tab
    EditText mIOSector;
    EditText mIOBlock;
    EditText mIOResult;


    //UI elements on ACCESS tab
    EditText mAccessSector;
    EditText mAccessKeyA;
    EditText mAccessKeyB;
    EditText mAccessBits;

    //UI elements on VALUE tab
    EditText mValueSector;
    EditText mValueBlock;
    EditText mValue;

    //Dialog element
    AlertDialog mTagDialog;


    EditText mTagUID;
    EditText mCardType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Tab host configuration
        TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec spec1=tabHost.newTabSpec("T1");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Auth");

        TabHost.TabSpec spec2=tabHost.newTabSpec("T2");
        spec2.setContent(R.id.tab2);
        spec2.setIndicator("Read/Write");

        TabHost.TabSpec spec3=tabHost.newTabSpec("T3");
        spec3.setContent(R.id.tab3);
        spec3.setIndicator("Value");

        TabHost.TabSpec spec4=tabHost.newTabSpec("T4");
        spec4.setContent(R.id.tab4);
        spec4.setIndicator("Access");

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        tabHost.addTab(spec4);


        //Link variables with UI elements on XML file (AUTH tab)
        mAuthKeyA = ((EditText) findViewById(R.id.editTextKeyA));
        mAuthKeyB = ((EditText) findViewById(R.id.editTextKeyB));
        mAuthSector = ((EditText) findViewById(R.id.editTextAuthSector));
        mAuthRadioGroup = ((RadioGroup) findViewById(R.id.keySelectorRadioGroup));
        //Click listener for button (AUTH tab)
        findViewById(R.id.authWithSelectedKey).setOnClickListener(mTagAuthenticate);


        //Link variables with UI elements on XML file (READ/WRITE tab)
        mIOSector = ((EditText) findViewById(R.id.editTextIOSector));
        mIOBlock = ((EditText) findViewById(R.id.editTextIOBlock));
        mIOResult = ((EditText) findViewById(R.id.editTextIOResult));
        //Click listener for button (READ/WRITE tab)
        findViewById(R.id.readButton).setOnClickListener(mTagRead);
        findViewById(R.id.writeButton).setOnClickListener(mTagWrite);

        //Link variables with UI elements on XML file (ACCESS tab)
        mAccessSector = ((EditText) findViewById(R.id.editTextAccessSector));
        mAccessKeyA = ((EditText) findViewById(R.id.editTextAccessKeyA));
        mAccessKeyB = ((EditText) findViewById(R.id.editTextAccessKeyB));
        mAccessBits = ((EditText) findViewById(R.id.editTextAccessBits));
        //Click listener for button (ACCESS tab)
        findViewById(R.id.readAccessButton).setOnClickListener(mTagReadAccess);
        findViewById(R.id.writeAccessButton).setOnClickListener(mTagWriteAccess);

        //Link variable with UI elements on XML file (VALUE tab)
        mValueSector = ((EditText) findViewById(R.id.editTextValueSector));
        mValueBlock = ((EditText) findViewById(R.id.editTextValueBlock));
        mValue = ((EditText) findViewById(R.id.editTextValue));
        //Click listener for button (VALUE tab)
        findViewById(R.id.buttonValueRead).setOnClickListener(mTagReadValue);
        findViewById(R.id.buttonValueWrite).setOnClickListener(mTagWriteValue);

        /*
        mTagUID = ((EditText) findViewById(R.id.tag_uid));
        mCardType = ((EditText) findViewById(R.id.cardtype));
        */


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



    /*

    METHODS TRIGGERED BY BUTTONS - SET FLAG TO CURRENT ACTION

    */


    //User wants to authenticate
    private View.OnClickListener mTagAuthenticate = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            //Set auth flag to true
            enableTagAuthMode();

            //Prepare message
            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "Se va a autenticar el sector "+mAuthSector.getText()+" con la llave "+keyName+" ("+keyValue+")";

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_authenticate))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });
            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };


    //User wants to read a block
    private View.OnClickListener mTagRead = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            enableTagReadMode();
            //Prepare message
            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mIOSector.getText()+" con la llave "+keyName+" ("+keyValue+")";

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_read))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });
            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };


    //User wants to write a block
    private View.OnClickListener mTagWrite = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            enableTagWriteMode();

            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mIOSector.getText()+" con la llave "+keyName+" ("+keyValue+")";


            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });

            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };


    //User wants to read access bit
    private View.OnClickListener mTagReadAccess = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {

            enableReadAccessMode();

            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "Leer accesos: Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mAccessSector.getText()+" con la llave "+keyName+" ("+keyValue+")";


            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });

            mTagDialog = builder.create();
            mTagDialog.show();


        }
    };

    //User wants to write access bits
    private View.OnClickListener mTagWriteAccess = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            enableWriteAccessMode();

            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "ESCRIBIR ACCESOS: Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mIOSector.getText()+" con la llave "+keyName+" ("+keyValue+")";


            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });

            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };

    //User wants to read a value
    private View.OnClickListener mTagReadValue = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            enableReadValueMode();

            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "LEER VALOR: Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mIOSector.getText()+" con la llave "+keyName+" ("+keyValue+")";


            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });

            mTagDialog = builder.create();
            mTagDialog.show();
        }
    };

    //User wants to write a value
    private View.OnClickListener mTagWriteValue = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0) {
            enableWriteValueMode();

            Editable keyValue = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? mAuthKeyA.getText() : mAuthKeyB.getText());
            String keyName = (R.id.radioButtonKeyA == mAuthRadioGroup.getCheckedRadioButtonId() ? "A" : "B");
            String msg = "ESCRIBIR VALOR: Se va a autenticar el bloque "+mIOBlock.getText()+" en el sector "+mIOSector.getText()+" con la llave "+keyName+" ("+keyValue+")";


            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this)
                    .setTitle(getString(R.string.ready_to_write))
                    .setMessage(msg)
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
                            enableTagReadUDIDMode();
                        }
                    });

            mTagDialog = builder.create();
            mTagDialog.show();


        }
    };




    /*
        FLAG SETTERS
     */

    private void enableTagReadUDIDMode(){
        currentMode = Mode.INFOMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    //This mode lets the user know general information about the tag
    private void enableTagWriteMode()
    {
        currentMode = Mode.WRITEMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableTagReadMode()
    {
        currentMode = Mode.READMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    //This mode lets the user authenticate sectors on a tag
    private void enableTagAuthMode()
    {

        currentMode = Mode.AUTHMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableReadAccessMode(){
        currentMode = Mode.READACCESSMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableWriteAccessMode(){
        currentMode = Mode.WRITEACCESSMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableReadValueMode(){
        currentMode = Mode.READVALUEMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
    }

    private void enableWriteValueMode(){
        currentMode = Mode.WRITEVALUEMODE;
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mReadWriteTagFilters, mTechList);
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


        switch (currentMode){
            case INFOMODE:
                //No current support for info mode
                break;
            case AUTHMODE:
                resolveAuthIntent(intent);
                break;
            case READMODE:
                resolveReadIntent(intent);
                break;
            case WRITEMODE:
                resolveWriteIntent(intent);
                break;
            case READACCESSMODE:
                resolveReadAccessIntent(intent);
                break;
            case WRITEACCESSMODE:
                resolveWriteAccessIntent(intent);
                break;
            case READVALUEMODE:
                resolveReadValueIntent(intent);
                break;
            case WRITEVALUEMODE:
                resolveWriteValueIntent(intent);
                break;
            case INCREMENTVALUEMODE:
                //Not supported yet
                break;
            case DECREMENTVALUEMODE:
                //Not supported yet
                break;
        }

        //Hide dialog
        mTagDialog.cancel();
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
                String hexUID = HexStringUtils.getHexString(tagUID, tagUID.length);
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
                    boolean auth;
                    String hexkey = "";
                    int selectedRadioButton = mAuthRadioGroup.getCheckedRadioButtonId();



                    int sector = Integer.valueOf(mIOSector.getText().toString());


                    //int sector = mfc.blockToSector(Integer.valueOf(mIOBlock.getText().toString()));
                    byte[] datakey;



                    if (selectedRadioButton == R.id.radioButtonKeyA){
                        hexkey = mAuthKeyA.getText().toString();
                        datakey = HexStringUtils.hexStringToByteArray(hexkey);
                        auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                    }else{
                        hexkey = mAuthKeyB.getText().toString();
                        datakey = HexStringUtils.hexStringToByteArray(hexkey);
                        auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                    }


                    if(auth){
                        //Get block to read (convert to 0-3 value)
                        //int bloque = Integer.valueOf(mIOBlock.getText().toString());
                        int readBlock = Integer.valueOf(mIOBlock.getText().toString());
                        int bloque = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, readBlock));


                        //Read block from tag
                        byte[] dataread = mfc.readBlock(bloque);

                        //Convert block into string
                        String blockread = HexStringUtils.getHexString(dataread, dataread.length);
                        //Update UI with read data
                        mIOResult.setText(blockread);

                        Log.i(TAG, "Bloque Leido: " + blockread);
                        Toast.makeText(this,"Lectura de bloque EXITOSA.", Toast.LENGTH_LONG).show();

                    }else{ // Authentication failed - Handle it
                        Toast.makeText(this,"Lectura de bloque FALLIDA dado autentificación fallida.",Toast.LENGTH_LONG).show();
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
                boolean auth;
                String hexkey = "";
                int id = mAuthRadioGroup.getCheckedRadioButtonId();
                //int bloque = Integer.valueOf(mIOBlock.getText().toString());
                //int sector = mfc.blockToSector(bloque);

                int sector = Integer.valueOf(mIOSector.getText().toString());
                int readBlock = Integer.valueOf(mIOBlock.getText().toString());
                int bloque = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, readBlock));

                byte[] datakey;



                if (id == R.id.radioButtonKeyA){
                    hexkey = mAuthKeyA.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }
                else{
                    hexkey = mAuthKeyB.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }



                if(auth){
                    //String strdata = mDatatoWrite.getText().toString();

                    //Get data from user, strip spaces
                    String strdata = mIOResult.getText().toString().replaceAll("\\s+","");

                    //Convert it to byte array
                    byte[] datatowrite = HexStringUtils.hexStringToByteArray(strdata);
                    //Write block
                    mfc.writeBlock(bloque, datatowrite);

                    Toast.makeText(this,"Escritura a bloque EXITOSA.",Toast.LENGTH_LONG).show();


                }else{ // Authentication failed - Handle it
                    Toast.makeText(this,"Escritura a bloque FALLIDA dado autentificación fallida.",Toast.LENGTH_LONG).show();
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
                boolean auth;
                String hexkey = "";
                int id = mAuthRadioGroup.getCheckedRadioButtonId();
                int sector = Integer.valueOf(mAuthSector.getText().toString());
                byte[] datakey;

                if (id == R.id.radioButtonKeyA){
                    hexkey = mAuthKeyA.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }
                else{
                    hexkey = mAuthKeyB.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }


                if(auth){
                    Toast.makeText(this,"Autentificación de sector EXITOSA.",Toast.LENGTH_LONG).show();
                }else{ // Authentication failed - Handle it
                    Toast.makeText(this,"Autentificación de sector FALLIDA.",Toast.LENGTH_LONG).show();
                }
                mfc.close();
            }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    //Write access intent
    void resolveWriteAccessIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            try {
                mfc.connect();
                boolean auth;
                String hexkey = "";
                int id = mAuthRadioGroup.getCheckedRadioButtonId();

                //int bloque = 3; //ACCESS is always on block 3
                //int sector = mfc.blockToSector(bloque);

                int sector = Integer.valueOf(mAccessSector.getText().toString());
                int bloque = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, 3));

                byte[] datakey;



                if (id == R.id.radioButtonKeyA){
                    hexkey = mAuthKeyA.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }
                else{
                    hexkey = mAuthKeyB.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }

                if(auth){
                    //Get data from user
                    String keyAString = mAccessKeyA.getText().toString();
                    String keyBString = mAccessKeyB.getText().toString();
                    String accessBits = mAccessBits.getText().toString();

                    String stringToWrite = (keyAString+accessBits+keyBString).replaceAll("\\s+","");

                    System.out.println("SE VA A ESCRIBIR. A:"+keyAString+" B "+keyBString+" Access:"+accessBits);

                    /*byte[] keyA = HexStringUtils.hexStringToByteArray(keyAString);
                    byte[] keyB = HexStringUtils.hexStringToByteArray(keyBString);
                    byte[] access = HexStringUtils.hexStringToByteArray(accessBits);
                    */

                    byte[] dataToWrite = HexStringUtils.hexStringToByteArray(stringToWrite);

                    System.out.println(HexStringUtils.getHexString(dataToWrite, dataToWrite.length));

                    //Convert it to byte array
                    mfc.writeBlock(bloque, dataToWrite);

                    Toast.makeText(this,
                            "Escritura a bloque de acceso EXITOSA.",
                            Toast.LENGTH_LONG).show();



                }else{ // Authentication failed - Handle it
                    Toast.makeText(this,"Escritura a bloque de acceso FALLIDA dado autentificación fallida.",Toast.LENGTH_LONG).show();
                }

                mfc.close();
                mTagDialog.cancel();

            }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

        }
    }

//Read access intent
void resolveReadAccessIntent(Intent intent) {
    String action = intent.getAction();
    if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareClassic mfc = MifareClassic.get(tagFromIntent);

        try {
            mfc.connect();
            boolean auth;
            String hexkey = "";
            int selectedRadioButton = mAuthRadioGroup.getCheckedRadioButtonId();

            //int sector = mfc.blockToSector(Integer.valueOf(mAccessSector.getText().toString()));
            int sector = Integer.valueOf(mAccessSector.getText().toString());
            byte[] datakey;



            if (selectedRadioButton == R.id.radioButtonKeyA){
                hexkey = mAuthKeyA.getText().toString();
                datakey = HexStringUtils.hexStringToByteArray(hexkey);
                auth = mfc.authenticateSectorWithKeyA(sector, datakey);
            }else{
                hexkey = mAuthKeyB.getText().toString();
                datakey = HexStringUtils.hexStringToByteArray(hexkey);
                auth = mfc.authenticateSectorWithKeyB(sector, datakey);
            }


            if(auth){
                //Get block to read
                //int sector = Integer.valueOf(mAccessSector.getText().toString());
                int bloque = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, 3));
                //Read block from tag
                byte[] dataread = mfc.readBlock(bloque);


                System.out.println("DATO LEIDO: "+dataread);

                //Split byte array into KeyA, access bits and KeyB
                byte[] keyABits = Arrays.copyOfRange(dataread, 0, 6);// Key A goes from 0 to 5
                byte[] accessBits = Arrays.copyOfRange(dataread, 6, 10); // Access bits go from 6 to 9
                byte[] keyBBits = Arrays.copyOfRange(dataread, 10, dataread.length);// Key B goes from 10 15
                //Convert bits into strings
                String keyA = HexStringUtils.getHexString(keyABits, keyABits.length);
                String access = HexStringUtils.getHexString(accessBits, accessBits.length);
                String keyB = HexStringUtils.getHexString(keyBBits, keyABits.length);
                //Update UI with read data
                mAccessKeyA.setText(keyA);
                mAccessBits.setText(access);
                mAccessKeyB.setText(keyB);
                //Print the output
                Log.i(TAG, "ESCRITURA DE ACCESO. A:" + keyA+" ACCESS:"+access+" B:"+keyB);
                //Notify the user that operation was successful
                Toast.makeText(this, "Lectura de bloque EXITOSA.", Toast.LENGTH_LONG).show();

            // Authentication failed
            }else{
                Toast.makeText(this, "Lectura de bloque FALLIDA dado autentificación fallida.", Toast.LENGTH_LONG).show();
            }

            mfc.close();
            mTagDialog.cancel();

        }catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }



    }
}

    //Read value intent
    void resolveReadValueIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            try {
                mfc.connect();
                boolean auth;
                String hexkey = "";
                int selectedRadioButton = mAuthRadioGroup.getCheckedRadioButtonId();

                //int sector = mfc.blockToSector(Integer.valueOf(mAccessSector.getText().toString()));
                int sector = Integer.valueOf(mValueSector.getText().toString());
                byte[] datakey;



                if (selectedRadioButton == R.id.radioButtonKeyA){
                    hexkey = mAuthKeyA.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }else{
                    hexkey = mAuthKeyB.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }


                if(auth){
                    //Get block to read
                    int blockRead = Integer.valueOf(mValueBlock.getText().toString());
                    int bloque = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, blockRead));
                    //Read block from tag
                    byte[] dataread = mfc.readBlock(bloque);
                    //Get the first 4 bytes (this is where the value is stored)
                    byte[] value =  Arrays.copyOfRange(dataread, 0, 4);


                    System.out.println("VAL IS: "+HexStringUtils.getHexString(value, value.length));

                    //Convert bits into strings
                    String strData = Long.toString(HexStringUtils.byteArrayToInt(value));
                    //Update UI with read data
                    mValue.setText(strData);


                    System.out.println("DATO LEIDO: "+dataread);

                    //Notify the user that operation was successful
                    Toast.makeText(this, "Lectura de bloque EXITOSA.", Toast.LENGTH_LONG).show();

                    // Authentication failed
                }else{
                    Toast.makeText(this, "Lectura de bloque FALLIDA dado autentificación fallida.", Toast.LENGTH_LONG).show();
                }

                mfc.close();
                mTagDialog.cancel();

            }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }



        }
    }

    //Write value intent
    void resolveWriteValueIntent(Intent intent) {
        System.out.println("HACIENDO COMO QUE ESCRIBO...");


        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            try {
                mfc.connect();
                boolean auth;
                String hexkey = "";
                int selectedRadioButton = mAuthRadioGroup.getCheckedRadioButtonId();

                //int sector = mfc.blockToSector(Integer.valueOf(mAccessSector.getText().toString()));
                int sector = Integer.valueOf(mValueSector.getText().toString());
                byte[] datakey;



                if (selectedRadioButton == R.id.radioButtonKeyA){
                    hexkey = mAuthKeyA.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyA(sector, datakey);
                }else{
                    hexkey = mAuthKeyB.getText().toString();
                    datakey = HexStringUtils.hexStringToByteArray(hexkey);
                    auth = mfc.authenticateSectorWithKeyB(sector, datakey);
                }


                if(auth){
                    //Get block entered by the user
                    int blockRead = Integer.valueOf(mValueBlock.getText().toString());
                    //Convert to absolute block direction
                    int block = Integer.valueOf(SectorBlockUtils.getAbsoluteBlock(sector, blockRead));


                    //Read int from field
                    int intValue = Integer.valueOf(mValue.getText().toString());
                    //Convert to byte[]
                    byte[] byteValue = AlgoritmoByte.magia(intValue, (byte)block);

                    //Write to block
                    mfc.writeBlock(block, byteValue);

                    //Notify the user that operation was successful
                    Toast.makeText(this, "Escritura de bloque EXITOSA.", Toast.LENGTH_LONG).show();

                    // Authentication failed
                }else{
                    Toast.makeText(this, "Escritura de bloque FALLIDA dado autentificación fallida.", Toast.LENGTH_LONG).show();
                }

                mfc.close();
                mTagDialog.cancel();

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


    /* Called when the system is about to start resuming a previous activity. */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause: " + getIntent());
        mNfcAdapter.disableForegroundDispatch(this);

    }



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




}

