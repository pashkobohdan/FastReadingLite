package com.pashkobohdan.fastreadinglite.library.bookTextWorker;

import com.pashkobohdan.fastreadinglite.data.dto.DBBookDTO;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Bohdan Pashko on 02.02.17.
 */

public class BookInfosList {
    private static final List<DBBookDTO> bookInfos = new LinkedList<>();

    public static boolean add(DBBookDTO bookInfo){
        if (bookInfos.contains(bookInfo)){
            return false;
        }else{
            bookInfos.add(bookInfo);
            return true;
        }
    }

//    public static DBBookDTO get(File file){
//        for(DBBookDTO bookInfo : bookInfos){
//            if(bookInfo.getFile().getAbsolutePath().equals(file.getAbsolutePath())){
//                return bookInfo;
//            }
//        }
//
//        return null;
//    }

    public static List<DBBookDTO> getAll(){
        return bookInfos;
    }
}
