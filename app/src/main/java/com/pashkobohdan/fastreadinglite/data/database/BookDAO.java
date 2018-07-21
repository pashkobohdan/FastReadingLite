package com.pashkobohdan.fastreadinglite.data.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.pashkobohdan.fastreadinglite.data.dto.DBBookDTO;

import java.util.List;

@Dao
public interface BookDAO {

    @Query("SELECT * FROM DBBookDTO")
    List<DBBookDTO> getAllBooks();

    @Query("SELECT * FROM DBBookDTO WHERE id LIKE :bookId")
    List<DBBookDTO> bookByIdList(long bookId) ;

    @Delete
    void deleteBook(DBBookDTO books);

    @Query("UPDATE DBBookDTO SET currentWordNumber=:currentWord WHERE id = :bookId")
    void updateBookPosition(int currentWord, int bookId);

    @Query("UPDATE DBBookDTO SET author=:newAuthor WHERE id = :bookId")
    void updateBookAuthor(int newAuthor, int bookId);

    @Query("UPDATE DBBookDTO SET color=:newColor WHERE id = :bookId")
    void updateBookColor(int newColor, int bookId);

    @Query("UPDATE DBBookDTO SET currentSpeed=:newSpeed WHERE id = :bookId")
    void updateBookSpeed(int newSpeed, int bookId);

    @Query("UPDATE DBBookDTO SET lastOpeningDate=:newDate WHERE id = :bookId")
    void updateBookLasOpenDate(int newDate, int bookId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllBookDTO(DBBookDTO ... books);
}
