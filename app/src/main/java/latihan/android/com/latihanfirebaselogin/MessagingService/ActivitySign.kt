package latihan.android.com.latihanfirebaselogin.MessagingService

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import latihan.android.com.latihanfirebaselogin.R

class ActivitySign: AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private val TAG = "ActivitySign"
    private val RC_SIGN_IN = 9001
    private var mSignInButton: SignInButton? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mFirebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        mSignInButton = findViewById<View>(R.id.sign_in_button) as SignInButton
        mSignInButton!!.setOnClickListener (this)
        //configure GoogleSign
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        mGoogleApiClient = GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build()
        mFirebaseAuth = FirebaseAuth.getInstance()

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
       Log.d(TAG, "onConnectionFailed"+"$p0")
        Toast.makeText(this, "Google Play"+"Services error.", Toast.LENGTH_SHORT).show()
    }

    override fun onClick(v: View) {
       when(v.id){
           R.id.sign_in_button-> signIn()
       }
    }

    private fun signIn() {
       val signIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess){
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }else{
                Log.e(TAG, "Google Sign-in failed")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle" + account.id)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mFirebaseAuth!!.signInWithCredential(credential).addOnCompleteListener(this) {task->
            Log.d(TAG, "Sign With Credential: oncomplete"+ task.isSuccessful )
            if (!task.isSuccessful){
                Log.w(TAG, " Sign With Credential", task.exception)
                Toast.makeText(this@ActivitySign, "Auth success", Toast.LENGTH_SHORT).show()
            }else{
                startActivity(Intent(this@ActivitySign, MainMessage::class.java))
                finish()
            }

        }

    }
}