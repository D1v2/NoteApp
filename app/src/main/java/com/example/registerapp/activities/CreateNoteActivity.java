package com.example.registerapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.registerapp.R;
import com.example.registerapp.database.NotesDatabase;
import com.example.registerapp.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNotetitle,inputNotesubtitle,inputNoteText;
    private TextView textDatetime;

    private View viewsubtitleindicator;
    private String selectedNoteColor;

    private TextView textWebURL;
    private LinearLayout layoutWebURl;
    private AlertDialog dialogAddUrl;

    private static final int REQUEST_CODE_STORAGE_PERMISSION=1;
    private static final int REQUEST_CODE_SELECT_IMAGE=2;

    private ImageView imageNote;
    private String selectedImagePath;

    private Note alreadyAvailableNote;

    private AlertDialog dialogDeleteNote;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        ImageView imageback=findViewById(R.id.imageback);
        imageback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        inputNotetitle=findViewById(R.id.inputnotetitle);
        inputNotesubtitle=findViewById(R.id.inputNoteSubtitle);
        inputNoteText=findViewById(R.id.inputNote);
        textDatetime=findViewById(R.id.textdatetime);
        viewsubtitleindicator=findViewById(R.id.viewsubtitleindicator);
        imageNote=findViewById(R.id.imageNote);
        textWebURL=findViewById(R.id.textWebUrl);
        layoutWebURl=findViewById(R.id.layoutWebUrl);

        textDatetime.setText(
                new SimpleDateFormat("EEEE,dd MMMM yyyy HH:mm,a", Locale.getDefault()).format(new Date())
        );

        ImageView imageSave=findViewById(R.id.imagesave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNotes();
            }
        });

        selectedImagePath="";

        if(getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote=(Note)getIntent().getSerializableExtra("note");
            setViewonUpdate();
        }
        findViewById(R.id.imageRemoveWebUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebURL.setText(null);
                layoutWebURl.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath="";
            }
        });

        selectedNoteColor="#122A2E";

        if (getIntent().getBooleanExtra("isFromQuickListener",false)){
            String type=getIntent().getStringExtra("quickActionType");
            if(type!=null){
                if(type.equals("image")){
                    selectedImagePath=getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                } else if (type.equals("Url")) {
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURl.setVisibility(View.VISIBLE);
                }
            }
        }
        initMiscallaneous();
        setSubtitleIndicatorColor();
    }

    private void setViewonUpdate(){
        inputNotetitle.setText(alreadyAvailableNote.getTitle());
        inputNotesubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDatetime.setText(alreadyAvailableNote.getDatetime());
        if(alreadyAvailableNote.getImagePath()!=null && !alreadyAvailableNote.getImagePath().trim().isEmpty()){
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath=alreadyAvailableNote.getImagePath();
        }
        if(alreadyAvailableNote.getWeblink()!= null && !alreadyAvailableNote.getWeblink().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWeblink());
            layoutWebURl.setVisibility(View.VISIBLE);
        }
    }

    private void saveNotes(){
        if(inputNotetitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note Title Can't be Empty",Toast.LENGTH_SHORT).show();
            return;
        }
        else if(inputNotesubtitle.getText().toString().trim().isEmpty() && inputNoteText.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note  Can't be Empty",Toast.LENGTH_SHORT).show();
            return;
        }
        final Note note=new Note();
        note.setTitle(inputNotetitle.getText().toString());
        note.setSubtitle(inputNotesubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDatetime(textDatetime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if(layoutWebURl.getVisibility()==View.VISIBLE){
            note.setWeblink(textWebURL.getText().toString());
        }

        if(alreadyAvailableNote!=null){
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                Intent intent=new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }

    private void initMiscallaneous(){
        final LinearLayout layoutMiscallaneous=findViewById(R.id.layoutmiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior=BottomSheetBehavior.from(layoutMiscallaneous);
        layoutMiscallaneous.findViewById(R.id.textMescallaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bottomSheetBehavior.getState()!=BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else{
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1=layoutMiscallaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2=layoutMiscallaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3=layoutMiscallaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4=layoutMiscallaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5=layoutMiscallaneous.findViewById(R.id.imageColor5);

        layoutMiscallaneous.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor="#122A2E";
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });


        layoutMiscallaneous.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor="#6E8996";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscallaneous.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor="#FF4842";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscallaneous.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor="#3A52Fc";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscallaneous.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor="#000000";
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);
                setSubtitleIndicatorColor();
            }
        });

        if(alreadyAvailableNote!=null && alreadyAvailableNote.getColor()!=null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            switch (alreadyAvailableNote.getColor()){
                case "#6E8996":
                    layoutMiscallaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layoutMiscallaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52Fc":
                    layoutMiscallaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMiscallaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layoutMiscallaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(CreateNoteActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                }
                else {
                    selectImage();
                }
            }
        });
        layoutMiscallaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddUrlDialog();
            }
        });

        if(alreadyAvailableNote!=null){
            layoutMiscallaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscallaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
                }
            });
        }

    }

    private void showDeleteNoteDialog(){
        if(dialogDeleteNote==null){
            AlertDialog.Builder builder=new AlertDialog.Builder(CreateNoteActivity.this);
            View view=LayoutInflater.from(this).inflate(R.layout.layout_delete_note,(ViewGroup)findViewById(R.id.layoutDeleteNoteContainer));
            builder.setView(view);
            dialogDeleteNote=builder.create();
            if(dialogDeleteNote.getWindow()!=null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent=new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                }
            });
            view.findViewById(R.id.textCancle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor(){

        GradientDrawable gradientDrawable=(GradientDrawable)viewsubtitleindicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));

    }
    private void selectImage(){
        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_SELECT_IMAGE && resultCode==RESULT_OK){
            if(data!=null){
                Uri selectedimageUri=data.getData();
                if(selectedimageUri!=null){
                    try{

                        InputStream inputStream=getContentResolver().openInputStream(selectedimageUri);
                        Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                        selectedImagePath=getPathfromUri(selectedimageUri);

                    }catch (Exception e){
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getPathfromUri(Uri contenturi){
        String filepath;
        Cursor cursor=getContentResolver().query(contenturi,null,null,null);
        if(cursor==null){
            filepath=contenturi.getPath();

        }else{
            cursor.moveToFirst();
            int index=cursor.getColumnIndex("_data");
            filepath=cursor.getString(index);
            cursor.close();
        }
        return filepath;
    }

    private void showAddUrlDialog(){
        if(dialogAddUrl == null){
            AlertDialog.Builder builder=new AlertDialog.Builder(CreateNoteActivity.this);
            View view= LayoutInflater.from(this).inflate(R.layout.layout_add_url,(ViewGroup)findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddUrl=builder.create();
            if(dialogAddUrl.getWindow()!=null){
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            final EditText inputUrl=view.findViewById(R.id.inputUrl);
            inputUrl.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(inputUrl.getText().toString().trim().isEmpty()){
                        Toast.makeText(CreateNoteActivity.this,"Enter URL",Toast.LENGTH_SHORT).show();
                    }
                    else if(!Patterns.WEB_URL.matcher(inputUrl.getText().toString()).matches()){
                        Toast.makeText(CreateNoteActivity.this,"Enter Valid Url",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        textWebURL.setText(inputUrl.getText().toString());
                        layoutWebURl.setVisibility(View.VISIBLE);
                        dialogAddUrl.dismiss();
                    }
                }
            });
            view.findViewById(R.id.textCancle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddUrl.dismiss();
                }
            });
        }
        dialogAddUrl.show();
    }
}