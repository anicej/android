package org.owntracks.android.data.repos;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.model.messages.BLEObject;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocationRepo {
    private final EventBus eventBus;
    private Location currentLocation;
    private ArrayList<BLEObject> bleObjects=new ArrayList<>();

    @Inject
    public LocationRepo(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Nullable
    public Location getCurrentLocation() {
        return this.currentLocation;
    }

    public boolean hasLocation() {
        return this.currentLocation != null;
    }

    public long getCurrentLocationTime() {
        return hasLocation() ? this.currentLocation.getTime() : 0;
    }

    public void setCurrentLocation(@NonNull Location l) {
        this.currentLocation = l;
        eventBus.postSticky(l);
    }

    public void setBleObject(@NonNull ArrayList<BLEObject> bleObjects) {
//        Log.e("TAG", "setbleeeeeeeee: "+bleObjects.size());

        this.bleObjects.clear();
        this.bleObjects.addAll(bleObjects);
        eventBus.postSticky(bleObjects);
    }

    public ArrayList<BLEObject> getBleObjects() {
//        Log.e("TAG", "getBleObjects: "+bleObjects.size());
        return bleObjects;
    }
}
