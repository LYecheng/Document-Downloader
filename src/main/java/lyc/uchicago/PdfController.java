package lyc.uchicago;

/**
 * Created by lyc on 8/1/15.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

 http://www.programmableweb.com/
 http://www.faroo.com/
 http://www.faroo.com/hp/api/api.html
 http://www.faroo.com/api?q=iphone&start=1&length=10&l=en&src=web&f=json
 http://jsonviewer.stack.hu/
 */

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * FXML Controller class
 *
 * @author ag
 */
public class PdfController implements Initializable {

    @FXML
    private TextField txtSearch;

    @FXML
    private TextArea msgText;

    @FXML
    private Button btnGo;

    @FXML
    private ListView<String> lstView;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblTitle;

    @FXML
    private TableView<DownTask> table;

    private String strDirSave;

    @FXML
    private Button btnSelect;

    @FXML
    private TextField fileType;

    private String notifMessage;

    public static int fileIndex=0;

    @FXML
    private void btnSelect_go(ActionEvent event) {

        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("This is my file ch");

        //Show open file dialog
        File file = directoryChooser.showDialog(null);

        if (file != null) {
            btnSelect.setText(file.getPath());
            strDirSave = file.getAbsolutePath();
        }

    }

    //this is not a good implementation because it only searching one page of a google search result. You should either
    //use a webservice like faroo (see TimesMain for example of web-service) or you should search multiple pages (scrape) google for results.
    //your should be returning approximately 50 documents per search.
    public class GetLinksToPdfsTask extends Task<ObservableList<String>> {

        private String filetypeStr = fileType.getText().toLowerCase();

        private String convertSpacesToPluses(String strOrig) {
            return strOrig.trim().replace(" ", "+");
        }

        protected String convertSpacesToUnderscores(String strOrig) {
            return strOrig.trim().replace(" ", "_");
        }

        @Override
        protected ObservableList<String> call() throws Exception {

            // ObservableList<String> sales = FXCollections.observableArrayList();
            updateMessage("Finding "+ filetypeStr + "s  . . .");

            File curDir = new File(strDirSave+"/"+convertSpacesToUnderscores(txtSearch.getText()));
            if(!curDir.exists()){
                curDir.mkdir();
            }
            strDirSave = strDirSave+"/"+convertSpacesToUnderscores(txtSearch.getText());

            String strUrl = "https://www.google.com/search?q=";
            strUrl += convertSpacesToPluses(txtSearch.getText());
            strUrl += "+filetype:"+filetypeStr+"&num=100";

            //this just simulates some work, but since it  and should be in background thread
            Document doc;
            ArrayList<String> strResults = new ArrayList<String>();
            try {
                doc = Jsoup.connect(strUrl).userAgent("Mozilla").ignoreHttpErrors(true).timeout(0).get();
                Elements hrefs = doc.select("a[href]");
                for (Element href : hrefs) {

                    //uses the jsoup library to strip the links from google.
                    String strRef = href.attr("abs:href");
                    if (strRef.contains("."+filetypeStr) && strRef.contains("https://www.google.com/url?q=")) {

                        String strPdfToDownload = strRef.substring(strRef.indexOf("https://www.google.com/url?q=") + 29, strRef.indexOf("."+filetypeStr) + filetypeStr.length()+1);
                        strResults.add(strPdfToDownload);

                    }

                }
                if (strResults == null || strResults.size() == 0) {
                    //the message property is bound to the label
                    notifMessage = "No data found for that search term...try again";
                    msgText.setText(notifMessage);
                    updateMessage(notifMessage);
                } else {
                    updateMessage(filetypeStr +"s found");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            //this will get returned and set to the lstView
            return FXCollections.observableArrayList(strResults);

        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        strDirSave = null;

        // table = new TableView<DownTask>();
        TableColumn<PdfController.DownTask, String> statusCol = new TableColumn("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<PdfController.DownTask, String>(
                "message"));
        statusCol.setPrefWidth(100);

        TableColumn<PdfController.DownTask, Double> progressCol = new TableColumn("Progress");
        progressCol.setCellValueFactory(new PropertyValueFactory<PdfController.DownTask, Double>(
                "progress"));

        progressCol.setPrefWidth(125);

        //this is the most important call
        progressCol
                .setCellFactory(ProgressBarTableCell.<PdfController.DownTask>forTableColumn());

        TableColumn<PdfController.DownTask, String> fileCol = new TableColumn("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<PdfController.DownTask, String>(
                "title"));
        fileCol.setPrefWidth(375);

        //add the cols
        table.getColumns().addAll(statusCol, progressCol, fileCol);

        btnGo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                if (strDirSave == null){
                    notifMessage = "You need to set an output dir!";
                    msgText.setText(notifMessage);
                    System.err.println(notifMessage);
                    return;
                } else if (txtSearch.getText() == null || txtSearch.getText().length() == 0) {
                    notifMessage = "You need to spefify a search term.";
                    msgText.setText(notifMessage);
                    System.err.println(notifMessage);
                    return;
                } else if (fileType.getText() == null || fileType.getText().length() == 0) {
                    notifMessage = "You need to spefify a file type.";
                    msgText.setText(notifMessage);
                    System.err.println(notifMessage);
                    return;
                }
                //you can instantiate a new Task each time if you want to "re-use" it, but once this intance is cancelled, you can't restart it again.
                final Task<ObservableList<String>> getLinksTask = new GetLinksToPdfsTask();

                table.getItems().clear();
                //the label is bound to the message property which we are updating inside the Task
                lblStatus.textProperty().bind(getLinksTask.messageProperty());
                //the button should be disabled once the task is done
                btnGo.disableProperty().bind(getLinksTask.runningProperty());
                //this is the value that returned by the task (the ObservableList<String>)
                lstView.itemsProperty().bind(getLinksTask.valueProperty());

                getLinksTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                    @Override
                    public void handle(WorkerStateEvent t) {

                        //this is the Future of the Future/Callable interface
                        ObservableList<String> observableList = getLinksTask.getValue();
                        if (observableList == null || observableList.size() == 0) {
                            return;
                        }

                        //add Tasks to the table
                        for (String str : getLinksTask.getValue()) {
                            table.getItems().add(new DownTask(str, strDirSave));
                        }

                        //fire up executor-service with the same number of threads as cores in this system
                        int cores = Runtime.getRuntime().availableProcessors();;
                        ExecutorService executor = Executors.newFixedThreadPool(cores, new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable r) {

                                Thread t = new Thread(r);
                                t.setDaemon(true);
                                return t;
                            }
                        });

                        //queue-up those items to the executor thread pool.
                        for (PdfController.DownTask pbarTask : table.getItems()) {
                            //these will execute in parallel so long as there are enough threads in the threadpool
                            executor.execute(pbarTask);
                        }

                    }
                });

                //this is what happens when you click the button. Because Task is a Runnable object, this is perfectly legitimate
                new Thread(getLinksTask).start();
            }
        });
    }

    class DownTask extends Task<Void> {

        private String pdfFrom, pdfTo;

        public DownTask(String pdfFrom, String pdfTo) {
            this.pdfFrom = pdfFrom;
            this.pdfTo = pdfTo;
        }

        private String toIndex(int index){
            if (index<10)
                return "000"+index;
            else if (index<100)
                return "00"+index;
            else if (index<1000)
                return "0"+index;
            else
                return ""+index;
        }

        @Override
        protected Void call() throws Exception {

            HttpDownUtil util = new HttpDownUtil();
            try {
                this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
                this.updateMessage("Waiting...");
                util.downloadFile(this.pdfFrom);

                InputStream inputStream = util.getInputStream();
                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(pdfTo + "/" + toIndex(fileIndex++) + "_" + util.getFileName());
                this.updateTitle(pdfTo + "/" + util.getFileName());

                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                long totalBytesRead = 0;
                int percentCompleted = 0;
                long fileSize = util.getContentLength();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    percentCompleted = (int) (totalBytesRead * 100 / fileSize);
                    updateProgress(percentCompleted, 100);
                }

                updateMessage("done");
                outputStream.close();
                util.disconnect();

            } catch (Exception ex) {
                ex.printStackTrace();
                this.cancel(true);
                table.getItems().remove(this);

            }
            return null;
        }
    }
}