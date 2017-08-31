/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package citereco;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author dwaipayan
 */

public class ReadPropertiesFile {
 
    private Properties prop = null;
     
    public ReadPropertiesFile(String propPath){
         
        FileInputStream is;
        try {
            this.prop = new Properties();
            is = new FileInputStream(propPath);
            prop.load(is);
        } catch (FileNotFoundException ex) {
            System.err.println("Prop file not found");
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     
    public Set<Object> getAllKeys(){
        Set<Object> keys = prop.keySet();
        return keys;
    }
     
    public String getPropertyValue(String key){
        return this.prop.getProperty(key);
    }
     
    public static void main(String args[]){
        if(args.length != 1) {
            System.err.println("Usage: <class> <path-of-the-prop-file>");
            args = new String[2];
            args[0] = "/home/dwaipayan/citereco/citereco.properties";
//            System.exit(1);
        }
        ReadPropertiesFile rpf = new ReadPropertiesFile(args[0]);
        Set<Object> keys = rpf.getAllKeys();
        for(Object k:keys){
            String key = (String)k;
            System.out.println(key+": "+rpf.getPropertyValue(key));
        }
    }
}
