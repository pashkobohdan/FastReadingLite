package com.pashkobohdan.fastreadinglite.library.ui.lists.booksList;

import com.pashkobohdan.fastreadinglite.library.bookTextWorker.BookInfo;

/**
 * Interface (can be used as Lambda expression) definition for a callback
 * to be invoked when needs a confirmation to make something (runIfConfirmed)
 *
 * Created by Bohdan Pashko on 24.01.17.
 */

public interface BookConfirmationEventListener {

    void run(BookInfo bookInfo, Runnable runIfConfirmed);

}