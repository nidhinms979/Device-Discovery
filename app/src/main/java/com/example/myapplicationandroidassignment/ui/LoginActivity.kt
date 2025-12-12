package com.example.myapplicationandroidassignment.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationandroidassignment.R
import com.example.myapplicationandroidassignment.auth.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }
        authManager = AuthManager(this)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE)
        btnGoogleSignIn.setOnClickListener {
            signIn()
        }

        // Check for silent authentication
        checkSilentAuth()
    }

    private fun checkSilentAuth() {
        if (authManager.isLoggedIn()) {
            if (authManager.shouldForceLogout()) {
                // Force logout if no network
                authManager.logout()
                showError("No network connection. Please sign in again.")
            } else {
                // Silent authentication successful
                navigateToHome()
            }
        }
    }

    private fun signIn() {
        if (!authManager.isNetworkAvailable()) {
            showError("No network connection available")
            return
        }

        showLoading(true)
        tvError.visibility = View.GONE

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Save token and user info
            val token = account.idToken ?: account.id ?: ""
            authManager.saveToken(
                token = token,
                email = account.email,
                name = account.displayName,
                userId = account.id
            )

            Log.d(TAG, "Sign in successful: ${account.email}")
            showLoading(false)
            navigateToHome()

        } catch (e: ApiException) {
            Log.w(TAG, "Sign in failed: ${e.statusCode}", e)
            showLoading(false)
            showError("Sign in failed: ${e.message}")
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGoogleSignIn.isEnabled = !show
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}