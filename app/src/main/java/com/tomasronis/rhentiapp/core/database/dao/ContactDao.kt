package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedContact
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CachedContact entity.
 */
@Dao
interface ContactDao {

    @Query("SELECT * FROM cached_contacts ORDER BY lastName ASC, firstName ASC")
    fun getAllContacts(): Flow<List<CachedContact>>

    @Query("SELECT * FROM cached_contacts WHERE id = :contactId")
    fun getContactById(contactId: String): Flow<CachedContact?>

    @Query("""
        SELECT * FROM cached_contacts
        WHERE firstName LIKE '%' || :query || '%'
        OR lastName LIKE '%' || :query || '%'
        OR email LIKE '%' || :query || '%'
        ORDER BY lastName ASC, firstName ASC
    """)
    fun searchContacts(query: String): Flow<List<CachedContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: CachedContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<CachedContact>)

    @Delete
    suspend fun deleteContact(contact: CachedContact)

    @Query("DELETE FROM cached_contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String)

    @Query("DELETE FROM cached_contacts")
    suspend fun deleteAllContacts()
}
