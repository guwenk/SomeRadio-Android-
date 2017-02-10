package com.github.guwenk.smuradio;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;


public class VoteActivity extends AppCompatActivity {

    private NodeList trackNodeList;
    //ProgressDialog pDialog;
    private List<Tracks> trackList = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private String filename;
    private String choose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        new ParseXML().execute();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_single_choice, names);
        listView.setAdapter(adapter);


        //<Поисковик>
        final EditText etFilter = (EditText)findViewById(R.id.filter);
        etFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                names.clear();
                String filter = etFilter.getText().toString();
                String trackName1, trackName2;
                for (int j = 0; j < trackList.size(); j++){
                    trackName1 = trackList.get(j).getArtist() + " " + trackList.get(j).getTitle();
                    trackName2 = trackList.get(j).getArtist() + " - " + trackList.get(j).getTitle();
                    if (trackName1.toUpperCase().contains(filter.toUpperCase()) || trackName2.toUpperCase().contains(filter.toUpperCase())){
                        names.add((j+1)+ ". " + trackList.get(j).getArtist() + " - " + trackList.get(j).getTitle());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //<Поисковик/>

        //<Кнопка голосования>
        Button btnVote = (Button)findViewById(R.id.buttonVote);
        btnVote.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                try {
                    StringBuilder sb = new StringBuilder(names.get(listView.getCheckedItemPosition()));
                    while (true){
                        if (sb.charAt(0) != '.'){
                            sb.deleteCharAt(0);
                        }else {
                            sb.deleteCharAt(0);
                            sb.deleteCharAt(0);
                            //Log.d("MusicName", sb.toString());
                            break;
                        }
                    }
                    for (int i = 0; i < trackList.size(); i++) {
                        if (Objects.equals(sb.toString(), trackList.get(i).getArtist() + " - " + trackList.get(i).getTitle())) {
                            filename = trackList.get(i).getFilename();
                            choose = trackList.get(i).getTitle();
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e){
                    filename = "";
                    choose = "";
                }

                if (!Objects.equals(filename, "") && filename != null) {
                    new VoteRequest().execute(filename, getString(R.string.request_address), getString(R.string.request_pass));
                    Toast.makeText(getApplicationContext(), getString(R.string.done) + choose, Toast.LENGTH_SHORT).show();
                    finish();
                } else{
                    Toast.makeText(getApplicationContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
                }
            }
        });
        //<Кнопка голосования/>
    }


    //Парсинг XML из сети
    protected class ParseXML extends AsyncTask<String, Void, Void>{
        boolean caughtException = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                URL url = new URL("http://" + getString(R.string.request_address)
                        + "/?pass=" + getString(R.string.request_pass)
                        + "&action=library&filename=Base");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                InputStream stream = connection.getInputStream();
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(new InputSource(stream));
                trackNodeList = doc.getElementsByTagName("Track");
            } catch (SocketTimeoutException e){
                caughtException = true;
            } catch (SAXException | ParserConfigurationException | IOException e) {
                e.printStackTrace();
            }
            if (trackNodeList != null) {
                ArrayList<String> names_mem = new ArrayList<>();
                for (int i = 0; i < trackNodeList.getLength(); i++) {
                    Element trackElement = (Element) trackNodeList.item(i);
                    Tracks tracks = new Tracks();
                    tracks.setArtist(trackElement.getAttribute("artist"));
                    tracks.setTitle(trackElement.getAttribute("title"));
                    tracks.setFilename(trackElement.getAttribute("filename"));
                    names_mem.add(trackElement.getAttribute("artist") + " - " + trackElement.getAttribute("title"));
                    trackList.add(tracks);
                }
                Collections.sort(names_mem, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareToIgnoreCase(s2);
                    }
                });
                for (int i = 0; i < names_mem.size(); i++){
                    names.add((i + 1) + ". " + names_mem.get(i));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //pDialog.dismiss();
            if (!caughtException) {
                ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar_orderMenu);
                pb.setVisibility(View.INVISIBLE);
                EditText et = (EditText)findViewById(R.id.filter);
                et.setVisibility(View.VISIBLE);
                ListView lv = (ListView)findViewById(R.id.listView);
                lv.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(), R.string.unable_to_connect_to_server, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
