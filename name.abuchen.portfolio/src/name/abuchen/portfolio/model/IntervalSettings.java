package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.RGB;

import name.abuchen.portfolio.util.ColorConversion;

public class IntervalSettings
{
    
    public static class SortIntervalSetting implements Comparator<IntervalSettings.IntervalSetting> 
    {
        
        @Override
        public int compare(IntervalSettings.IntervalSetting a, IntervalSettings.IntervalSetting b)
        {
            return a.getInterval() - b.getInterval();
        }
    }
    
    public class IntervalSetting
    {
        
        
        private Entry<Integer, TypedMap> setting; 
                
        public IntervalSetting(Entry<Integer, TypedMap> entry)
        {
            setting = entry;
        }

        public int getInterval()
        {
            return setting.getKey();
        }
        
        public RGB getRGB()
        {
            return ColorConversion.hex2RGB(setting.getValue().getString(IntervalSettings.C_COLOR));
        }
        
        public void setRGB(RGB color)
        {
            setting.getValue().putString(IntervalSettings.C_COLOR, ColorConversion.toHex(color));
        }
        
        public boolean getIsActive()
        {
            return setting.getValue().getBoolean(IntervalSettings.C_ISACTIVE);
        }
        
        public void setIsActive(boolean isActive)
        {
            setting.getValue().putBoolean(IntervalSettings.C_ISACTIVE, isActive);
        }
    }
    
    
    public static final String C_ISACTIVE = "ISACTIVE"; //$NON-NLS-1$
    public static final String C_COLOR = "COLOR"; //$NON-NLS-1$
  
    private Map<Integer, TypedMap> settings = new HashMap<>();
  
    public IntervalSettings()
    {
        this(new HashMap<>());
    }
    
    public IntervalSettings(Map<Integer, TypedMap> settings)
    {
        this.settings = settings;
    }
    
    public void clear()
    {
        settings.clear();           
    }
    
    public void add(int interval, RGB color, boolean isActive)
    {
        TypedMap typedMap = new TypedMap();
        typedMap.putBoolean(C_ISACTIVE, isActive);
        typedMap.putString(C_COLOR, ColorConversion.toHex(color));
        settings.put(interval, typedMap);       
    }
    
    public void remove(int interval)
    {
        settings.remove(interval);        
    }

    public IntervalSetting[] getAll()
    {
        ArrayList<IntervalSetting> tempList = new ArrayList<>();
        settings.entrySet().forEach(entry -> tempList.add(new IntervalSetting(entry)));
        return tempList.toArray(new IntervalSetting[0]);
    }
  
}
