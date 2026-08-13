package headless;

import arc.Files;
import arc.files.*;

/**
 * File system for headless mode. Single files fall back to classpath
 * resources (the game jar) when they do not exist on disk, like arc's
 * real HeadlessFiles implementation.
 */
public class HeadlessFiles implements Files{
    @Override public Fi get(String fileName, FileType type){
        return new Fi(fileName, type);
    }

    @Override public String getExternalStoragePath(){
        return "";
    }

    @Override public boolean isExternalStorageAvailable(){
        return false;
    }

    @Override public String getLocalStoragePath(){
        return "";
    }

    @Override public boolean isLocalStorageAvailable(){
        return true;
    }
}
