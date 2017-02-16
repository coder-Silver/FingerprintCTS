package com.cs.keystorewithfingerprint;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "keyStoreWithFingerprint";
    private static final int FINGERPRINT_PERMISSION_REQUEST_CODE = 0;
    private static final String KEY_NAME =  "my_fingerprint_key";
    private static final byte[] SECRET_BYTE_ARRAY = new byte[] {1, 2, 3, 4, 5, 6};

    private Button mBtnTest = null;
    private TextView mTvResult = null;
    private FingerprintManager mFingerprintManager = null ;
    private KeyguardManager mKeyguardManager = null ;
    private Cipher mCipher = null ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT},
                FINGERPRINT_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG,"permissions = "+permissions[0]+"\n"
        +"grantResults = "+grantResults[0]) ;
        if (requestCode == FINGERPRINT_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (null == mKeyguardManager || null == mFingerprintManager) {
                return ;
            }
            if (!mKeyguardManager.isKeyguardSecure()) {
                Log.i(TAG,"please set secure lock first") ;
                Toast.makeText(this,"please set secure lock first",Toast.LENGTH_LONG).show();
                mBtnTest.setEnabled(false);
                return ;
            }
            try{
                if (!mFingerprintManager.hasEnrolledFingerprints()) {
                    Toast.makeText(this,"Need enroll a fingerprint first",Toast.LENGTH_LONG).show() ;
                    Log.i(TAG,"Need enroll a fingerprint first") ;
                    mBtnTest.setEnabled(false);
                    return ;
                }

            }catch (SecurityException e){
                Log.e(TAG,"has enrolled fingerprints need permission") ;
                e.printStackTrace();
            }
        }else {
            Log.e(TAG,"Permission denied") ;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private class TestListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_test:
                    createKey();
                    prepareEncrypt() ;
                    if (tryEncrypt()) {
                        Log.e(TAG,"test failed ,Key accessible without auth") ;
                    }else {

                    }

                    break;
            }
        }
    }

    private void createKey() {
        try{
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore") ;
            keyStore.load(null);
            KeyGenerator keyGenerator  = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore") ;
            Log.i(TAG,"1111111111111111111") ;
            KeyGenParameterSpec.Builder keyParameterBuilder = new KeyGenParameterSpec.Builder(KEY_NAME,KeyProperties.PURPOSE_ENCRYPT
            | KeyProperties.PURPOSE_DECRYPT) ;
            Log.i(TAG,"222222222222222222222") ;
            keyParameterBuilder.setBlockModes(KeyProperties.BLOCK_MODE_CBC).setUserAuthenticationRequired(true).
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7) ;
            Log.i(TAG,"33333333333333333333331") ;
            keyGenerator.init(keyParameterBuilder.build());
            Log.i(TAG,"444444444444444444444444") ;
            SecretKey secreteKey = keyGenerator.generateKey() ;
            Log.i(TAG,"555555555555555555555555555555") ;
            Log.i(TAG,"secretKey = "+secreteKey.toString()) ;
        }catch (InvalidAlgorithmParameterException e){
            Log.e(TAG,"InvalidAlgorithmParameterException Create key failed") ;
            throw  new RuntimeException("create key failed") ;
        }catch(NoSuchAlgorithmException e){
            Log.e(TAG,"NoSuchAlgorithmException Create key failed") ;
            e.printStackTrace();
            throw  new RuntimeException("create key failed") ;
        }catch(KeyStoreException e) {
            Log.e(TAG,"KeyStoreException Create key failed") ;
            throw  new RuntimeException("create key failed") ;
        }catch (NoSuchProviderException e){
            Log.e(TAG,"NoSuchProviderException Create key failed") ;
            throw  new RuntimeException("create key failed") ;
        }catch (CertificateException e){
            Log.e(TAG,"CertificateException Create key failed") ;
            throw  new RuntimeException("create key failed") ;
        }catch (IOException e){
            Log.e(TAG,"IOException Create key failed") ;
            throw  new RuntimeException("create key failed") ;
        }
    }

    private boolean prepareEncrypt() {
        return internalEncrypt(false) ; // init cipher
    }
    private boolean  tryEncrypt() {
        return internalEncrypt(true) ;
    }
    private boolean internalEncrypt(boolean encrypt) {
        try{
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME,null);
            if (!encrypt) {
                if (null == mCipher) {
                    mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
                    mCipher.init(Cipher.ENCRYPT_MODE,secretKey);
                    Log.i(TAG,"cipher initialized ") ;
                }
            }else {
                if (null != mCipher) {
                    mCipher.doFinal(SECRET_BYTE_ARRAY) ;
                    Log.d(TAG,SECRET_BYTE_ARRAY.toString()) ;
                }
            }
            return true ;
        }catch (BadPaddingException | IllegalBlockSizeException e){
            Log.e(TAG,"encryption failed") ;
            return false ;
        }catch(KeyPermanentlyInvalidatedException e) {
            Log.w(TAG,"Encrypt key invalidated") ;
            Toast.makeText(this,"The key has been invalidated, please try again",Toast.LENGTH_LONG).show() ;
            createKey();
            return  false ;
        }catch (InvalidKeyException | NoSuchPaddingException | KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException | UnrecoverableKeyException e){
            Log.e(TAG,"encryption failed") ;
            throw new RuntimeException("Failed to init Cipher", e);
        }

    }
    /*
    private void showAuthenticationScreen() {
        mFingerprintDialog = new FingerprintAuthDialogFragment();
        mFingerprintDialog.show(getFragmentManager(), "fingerprint_dialog");
    }*/

    private void init() {
        mBtnTest = (Button) findViewById(R.id.button_test);
        mTvResult = (TextView) findViewById(R.id.tv_show_result);
        mBtnTest.setOnClickListener(new TestListener());
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE) ;
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE) ;
    }
}
