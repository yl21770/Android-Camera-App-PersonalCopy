package iss.workshop.ad_testcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private SharedPreferences prefs;
    private EditText usernamebox, passwordbox;
    private TextView errorText, registerlink;

    private CheckBox checkBox;
    private AppCompatButton loginbtn;
    private boolean rememberUser = false;

    private TextWatcher textWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getPreferences(MODE_PRIVATE);
        Intent intent = getIntent();
        if(intent != null){
            String prevActivity = intent.getStringExtra("prevActivity");
            if(prevActivity != null){
                if(prevActivity.equals("logout")){
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.commit();
                }
            }
        }
        checkLogin();
        usernamebox = findViewById(R.id.usernamebox);
        passwordbox = findViewById(R.id.passwordbox);
        errorText = findViewById(R.id.loginerror);
        checkBox = findViewById(R.id.rememberUser);
        checkBox.setOnClickListener(this);
        loginbtn = findViewById(R.id.loginbtn);
        loginbtn.setOnClickListener(this);
        registerlink = findViewById(R.id.registerlink);
        registerlink.setOnClickListener(this);

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                errorText.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        usernamebox.addTextChangedListener(textWatcher);
        passwordbox.addTextChangedListener(textWatcher);
    }

    @Override
    public void onClick(View v){
        int id = v.getId();

        if(id == R.id.loginbtn){
            String username = usernamebox.getText().toString();
            String password = passwordbox.getText().toString();
            if(username != null && !username.isEmpty() && password != null && !password.isEmpty()){
                getLogin(username, password);
            }
            else{
                errorText.setText(R.string.LOGINBLANK_ERR);
                errorText.setVisibility(View.VISIBLE);
            }
        }
        else if(id == R.id.registerlink){
            Intent launchRegister = new Intent(this, RegisterActivity.class);
            startActivity(launchRegister);
        }
        else if(id == R.id.forgotpassword){
            //to be implemented
        }
        else if(id == R.id.rememberUser){
            if (checkBox.isChecked()){
                rememberUser = true;
            }
            else{
                rememberUser = false;
            }
        }
    }

    private void checkLogin(){
        String username = prefs.getString("username", null);
        if(username != null && !username.isEmpty()){
            launchHome(username);
        }
    }

    private void getLogin(String username, String password){
        try{
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS).build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("userID", username)
                    .addFormDataPart("password", password)
                    .build();

            Request request = new Request.Builder()
                    .url(Constants.LOGIN_API)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //Timeout Error
                    runOnUiThread(() -> {
                        errorText.setText(getString(R.string.SERVER_DOWN_ERR));
                        errorText.setVisibility(View.VISIBLE);});
                }

                public void onResponse(Call call, final Response response) throws IOException {
                    if(response.code() == 200){
                        String username = response.body().string();
                        if(rememberUser == true){
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("username", username);
                            editor.commit();
                        }
                        launchHome(username);
                    }
                    else if(response.code() == 404){
                        //User not found
                        runOnUiThread(() -> {
                            errorText.setText(getString(R.string.LOGIN_ERR));
                            errorText.setVisibility(View.VISIBLE);});
                    }
                    else{
                        runOnUiThread(() -> {
                            errorText.setText(getString(R.string.SERVER_ERR));
                            errorText.setVisibility(View.VISIBLE);});
                    }
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void launchHome(String username){
        Intent launchHome = new Intent(this, HomeActivity.class);
        launchHome.putExtra("username", username);
        startActivity(launchHome);
        finish();
    }
}