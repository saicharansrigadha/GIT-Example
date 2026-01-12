package com.example.realmdatabase;

import static android.app.PendingIntent.getActivity;
import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity {

    EditText etName, etAge, etMobile;
    Button btSave;
    Realm realm;
    RecyclerView rvData;
    Listadapter listadapter;
    ArrayList<DataModal> dataModalList;
    boolean isFalse = false;
    long pos = 0;


    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etMobile = findViewById(R.id.etMobile);
        btSave = findViewById(R.id.btSave);
        rvData = findViewById(R.id.rvData);

        dataModalList = new ArrayList<>();
        Realm.init(this);
        realm = Realm.getDefaultInstance();

        rvData.setLayoutManager(new LinearLayoutManager(this));
        listadapter = new Listadapter(dataModalList, this);
        rvData.setAdapter(listadapter);
        dataModalList.addAll(realm.copyFromRealm(realm.where(DataModal.class).findAll()));

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etName.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter Name", Toast.LENGTH_SHORT).show();
                } else if (etAge.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter Age", Toast.LENGTH_SHORT).show();
                } else if (etMobile.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter Mobile", Toast.LENGTH_SHORT).show();
                } else {
                    if (isFalse) {
                        updateItem(pos);
                    } else {
                        saveData();
                        backupRealmNow();
                    }
                }
            }
        });
    }

    public void backupRealmNow() {
        Realm nowRealmForBackup = Realm.getDefaultInstance();
        int REQUESTCODE_WRITE = 100;
        int REQUESTCODE_READ = 200;

        String filePath = "";

        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MyDatabase");
            File exportRealmFile = new File(dir, "backup.realm");
            filePath = exportRealmFile.getPath();

            if (!dir.exists()) {
                dir.mkdirs();
            }

            if (!exportRealmFile.exists()) {
                exportRealmFile.mkdirs();
                if (exportRealmFile.exists()) {
                    Log.d("mkdirs", "Success to make dir");
                } else {
                    Log.d("mkdirs", "Failed to make dir");
                    if (PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                        Log.e("PermissionChecker", "WRITE_EXTERNAL_STORAGE PERMISSION_DENIED so request.");
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_WRITE);
                    } else {
                        Log.e("PermissionChecker", "WRITE_EXTERNAL_STORAGE PERMISSION_GRANTED");

                    }
                    if (PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                        Log.e("PermissionChecker", "READ_EXTERNAL_STORAGE PERMISSION_DENIED so request.");
                        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUESTCODE_READ);
                    } else {
                        Log.e("PermissionChecker", "READ_EXTERNAL_STORAGE PERMISSION_GRANTED");
                    }
                }
            }

            if (exportRealmFile.exists()) {
                exportRealmFile.delete();
                nowRealmForBackup.writeCopyTo(exportRealmFile);
                Log.d("Backup", "Success to backup " + filePath);
            } else {
                Log.d("Backup", "Failed to Backup");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            String msg = "File exported to Path: " + filePath;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            Log.d(TAG, msg);
            nowRealmForBackup.close();
        }
    }

    private void saveData() {
        final String name = etName.getText().toString();
        final int age = Integer.parseInt(etAge.getText().toString());
        final long mobile = Long.parseLong(etMobile.getText().toString());


        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Number id = realm.where(DataModal.class).max("id");
                long nextId = (id == null) ? 1 : id.longValue() + 1;

                DataModal existingItem = realm.where(DataModal.class).equalTo("name", name).findFirst();

                if (existingItem == null) {
                    DataModal dataModal = realm.createObject(DataModal.class, nextId);
                    dataModal.setName(name);
                    dataModal.setAge(age);
                    dataModal.setMobile(mobile);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(btSave.getWindowToken(), 0);

                Toast.makeText(MainActivity.this, "Item Added Successfully", Toast.LENGTH_SHORT).show();
                dataModalList.clear();
                dataModalList.addAll(realm.copyFromRealm(realm.where(DataModal.class).findAll()));
                listadapter.notifyDataSetChanged();
                etName.setText("");
                etAge.setText("");
                etMobile.setText("");
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Toast.makeText(MainActivity.this, "Error saving data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void editItem(DataModal modal) {
        isFalse = true;
        pos = modal.getId();
        etName.setText(modal.getName());
        etAge.setText(String.valueOf(modal.getAge()));
        etMobile.setText(String.valueOf(modal.getMobile()));
        btSave.setText("Update Data");
    }

    private void updateItem(final long id) {
        final String name = etName.getText().toString();
        final int age = Integer.parseInt(etAge.getText().toString());
        final long mobile = Long.parseLong(etMobile.getText().toString());

        DataModal nameItem = realm.where(DataModal.class).equalTo("name", name).notEqualTo("id", id).findFirst();


        DataModal mobileItem = realm.where(DataModal.class).equalTo("mobile", mobile).notEqualTo("id", id).findFirst();

        if (nameItem != null && mobileItem!= null) {
            Toast.makeText(MainActivity.this, "name already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

//        if (mobileItem != null) {
//            Toast.makeText(MainActivity.this, "mobile number already exists.", Toast.LENGTH_SHORT).show();
//            return;
//        }

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                DataModal dataModal = realm.where(DataModal.class).equalTo("id", id).findFirst();
                if (dataModal != null) {
                    dataModal.setName(name);
                    dataModal.setAge(age);
                    dataModal.setMobile(mobile);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(btSave.getWindowToken(), 0);  // Hide Keyboard

                Toast.makeText(MainActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                dataModalList.clear();
                dataModalList.addAll(realm.copyFromRealm(realm.where(DataModal.class).findAll()));
                listadapter.notifyDataSetChanged();

                etName.setText("");
                etAge.setText("");
                etMobile.setText("");
                isFalse = false;
                pos = 0;
                btSave.setText("Save Data");
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Toast.makeText(MainActivity.this, "Error updating data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    public void deleteItem(long id) {                                                                     //With name also works (final String name)
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                DataModal dataModal = realm.where(DataModal.class).equalTo("id", id).findFirst();
                if (dataModal != null) {
                    dataModal.deleteFromRealm();
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Toast.makeText(MainActivity.this, "Error deleting item: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
