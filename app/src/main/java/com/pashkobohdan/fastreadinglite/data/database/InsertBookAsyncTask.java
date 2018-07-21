package com.pashkobohdan.fastreadinglite.data.database;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.pashkobohdan.fastreadinglite.data.dto.DBBookDTO;
import com.pashkobohdan.fastreadinglite.library.bookTextWorker.BookInfosList;

public class InsertBookAsyncTask extends AsyncTask<Void, Void, Void>{

    private Runnable postCallback;
    private DBBookDTO bookDTO;

    public InsertBookAsyncTask(Runnable postCallback, DBBookDTO bookDTO) {
        this.postCallback = postCallback;
        this.bookDTO = bookDTO;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        BookDAOHolder.getDatabase().getBookDAO().insertAllBookDTO(bookDTO);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        new Handler(Looper.getMainLooper()).post(()-> {
            BookInfosList.add(bookDTO);
            postCallback.run();
        });
    }
}
