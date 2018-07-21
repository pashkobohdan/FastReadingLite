package com.pashkobohdan.fastreadinglite.library.ui.lists.booksList;

import com.pashkobohdan.fastreadinglite.data.dto.DBBookDTO;

/**
 * Interface (can be used as Lambda expression) definition for a callback
 * to be invoked when makes moething with BookInfo
 *
 * Created by Bohdan Pashko on 24.01.17.
 */

public interface BookEventListener {

    void run(DBBookDTO bookInfo);

}
