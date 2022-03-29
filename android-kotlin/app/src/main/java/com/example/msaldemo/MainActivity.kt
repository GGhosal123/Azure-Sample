package com.example.msaldemo

// Required dependencies
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import android.widget.Button
import android.widget.TextView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var msalApplication: ISingleAccountPublicClientApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)

        PublicClientApplication.createSingleAccountPublicClientApplication(
            this,
            R.raw.msal_auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalApplication = application
                    loadAccount()
                }

                override fun onError(exception: MsalException?) {
                    print(exception.toString())
                }
            })
    }

    private fun initializeUI() {
        val btnSignIn: Button = findViewById(R.id.btn_signIn)
        val btnRemoveAccount: Button = findViewById(R.id.btn_signOut)
        val btnCallGraph: Button = findViewById(R.id.btn_callGraph)

        // Full directory URL, in the form of https://login.microsoftonline.com/<tenant>
        val authority = ""

        btnSignIn.setOnClickListener{
            msalApplication!!.signIn(this, "", arrayOf("user.read"), getAuthInteractiveCallback())
        }

        btnRemoveAccount.setOnClickListener{
            msalApplication!!.signOut(object :
                ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    updateUI(null)
                    signOut()
                }

                override fun onError(exception: MsalException) {
                    displayError(exception)
                }
            })
        }

        btnCallGraph.setOnClickListener{
            msalApplication!!.acquireTokenSilentAsync(
                arrayOf("user.read"),
                authority,
                getAuthInteractiveCallback()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        initializeUI()
        loadAccount()
    }

    private fun loadAccount() {
        if (msalApplication == null) {
            return
        }

        msalApplication!!.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                updateUI(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    signOut()
                }
            }

            override fun onError(exception: MsalException) {
                val txtLog: TextView = findViewById(R.id.txt_log)
                txtLog.text = exception.toString()
            }
        })
    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                updateUI(authenticationResult.account)
                callGraphAPI(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                displayError(exception)
            }

            override fun onCancel() {
                // User canceled the authentication
            }
        }
    }

    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
            val queue = Volley.newRequestQueue(this)
            val request = object : JsonObjectRequest(
                Method.GET, "https://graph.microsoft.com/v1.0/me",
                JSONObject(),
                { response ->
                    val txtLog: TextView = findViewById(R.id.txt_log)
                    txtLog.text = response.toString()
                    val tokenExpiration: TextView = findViewById(R.id.token_expiration)
                    tokenExpiration.text = authenticationResult.expiresOn.toString()
                },
                { error ->
                    displayError(error)
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer "+authenticationResult.accessToken
                    return headers
                }
            }
            queue.add(request)
    }

    private fun displayError(exception: Exception) {
        val txtLog: TextView = findViewById(R.id.txt_log)
        txtLog.text = exception.toString()
    }

    private fun updateUI(account: IAccount?) {
        val btnSignIn: Button = findViewById(R.id.btn_signIn)
        val btnRemoveAccount: Button = findViewById(R.id.btn_signOut)
        val btnCallGraph: Button = findViewById(R.id.btn_callGraph)
        if (account != null) {
            btnSignIn.isEnabled = false
            btnRemoveAccount.isEnabled = true
            btnCallGraph.isEnabled = true
        } else {
            btnSignIn.isEnabled = true
            btnRemoveAccount.isEnabled = false
            btnCallGraph.isEnabled = false
        }
    }

    private fun signOut() {
        val txtLog: TextView = findViewById(R.id.txt_log)
        txtLog.text = ""

        val tokenExpiration: TextView = findViewById(R.id.token_expiration)
        tokenExpiration.text = ""
    }
}
