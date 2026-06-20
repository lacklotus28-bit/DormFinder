package com.rct.dormfinder.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {
        CachedDormitory.class,
        CachedBooking.class,
        CachedReview.class,
        CachedPayment.class
}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static AppDatabase instance;
    
    public abstract DormitoryDao dormitoryDao();
    
    public abstract BookingDao bookingDao();
    
    public abstract ReviewDao reviewDao();
    
    public abstract PaymentDao paymentDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "dormfinder_database"
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
