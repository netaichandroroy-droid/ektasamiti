package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY id DESC")
    fun getAllMembersFlow(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): Member?

    @Query("SELECT * FROM members WHERE memberCode = :code LIMIT 1")
    suspend fun getMemberByCode(code: String): Member?

    @Query("""
        SELECT * FROM members 
        WHERE name LIKE '%' || :query || '%' 
           OR memberCode LIKE '%' || :query || '%' 
           OR mobile LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members")
    fun getMemberCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getMemberCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT * FROM members")
    suspend fun getAllMembersList(): List<Member>

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}
