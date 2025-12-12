package com.example.myapplicationandroidassignment.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val ipAddress: String,
    val deviceName: String,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<Device>>
    
    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesOnce(): List<Device>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)
    
    @Update
    suspend fun updateDevice(device: Device)
    
    @Query("UPDATE devices SET isOnline = :status WHERE ipAddress = :ip")
    suspend fun updateDeviceStatus(ip: String, status: Boolean)
    
    @Delete
    suspend fun deleteDevice(device: Device)
    
    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()
}

@Database(entities = [Device::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "device_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DeviceRepository(private val deviceDao: DeviceDao) {
    val allDevices: Flow<List<Device>> = deviceDao.getAllDevices()
    
    suspend fun getAllDevicesOnce(): List<Device> = deviceDao.getAllDevicesOnce()
    
    suspend fun insertDevice(device: Device) {
        deviceDao.insertDevice(device)
    }
    
    suspend fun updateDevice(device: Device) {
        deviceDao.updateDevice(device)
    }
    
    suspend fun updateDeviceStatus(ip: String, status: Boolean) {
        deviceDao.updateDeviceStatus(ip, status)
    }
    
    suspend fun deleteDevice(device: Device) {
        deviceDao.deleteDevice(device)
    }
}