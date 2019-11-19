/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  ESP32 Erase plug-in

  Copyright (c) 2015 Hristo Gochkov (hristo at espressif dot com)

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.esp32.eraseflash;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;

import processing.app.PreferencesData;
import processing.app.Editor;
import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Platform;
import processing.app.Sketch;
import processing.app.tools.Tool;
import processing.app.helpers.ProcessUtils;
import processing.app.debug.TargetPlatform;

import org.apache.commons.codec.digest.DigestUtils;
import processing.app.helpers.FileUtils;

import cc.arduino.files.DeleteFilesOnShutdown;

/**
 * Example Tools menu entry.
 */
public class ESP32Erase implements Tool {
  Editor editor;


  public void init(Editor editor) {
    this.editor = editor;
  }


  public String getMenuTitle() {
    return "ESP32 Erase Flash";
  }

  private int listenOnProcess(String[] arguments){
      try {
        final Process p = ProcessUtils.exec(arguments);
        Thread thread = new Thread() {
          public void run() {
            try {
              InputStreamReader reader = new InputStreamReader(p.getInputStream());
              int c;
              while ((c = reader.read()) != -1)
                  System.out.print((char) c);
              reader.close();
              
              reader = new InputStreamReader(p.getErrorStream());
              while ((c = reader.read()) != -1)
                  System.err.print((char) c);
              reader.close();
            } catch (Exception e){}
          }
        };
        thread.start();
        int res = p.waitFor();
        thread.join();
        return res;
      } catch (Exception e){
        return -1;
      }
    }

  private void sysExec(final String[] arguments){
    Thread thread = new Thread() {
      public void run() {
        try {
          if(listenOnProcess(arguments) != 0){
            editor.statusError("Erase Flash failed!");
          } else {
            editor.statusNotice("Erase Flashed");
          }
        } catch (Exception e){
          editor.statusError("Erase Flash failed!");
        }
      }
    };
    thread.start();
  }

  private String getBuildFolderPath(Sketch s) {
    // first of all try the getBuildPath() function introduced with IDE 1.6.12
    // see commit arduino/Arduino#fd1541eb47d589f9b9ea7e558018a8cf49bb6d03
    try {
      String buildpath = s.getBuildPath().getAbsolutePath();
      return buildpath;
    }
    catch (IOException er) {
       editor.statusError(er);
    }
    catch (Exception er) {
      try {
        File buildFolder = FileUtils.createTempFolder("build", DigestUtils.md5Hex(s.getMainFilePath()) + ".tmp");
        return buildFolder.getAbsolutePath();
      }
      catch (IOException e) {
        editor.statusError(e);
      }
      catch (Exception e) {
        // Arduino 1.6.5 doesn't have FileUtils.createTempFolder
        // String buildPath = BaseNoGui.getBuildFolder().getAbsolutePath();
        java.lang.reflect.Method method;
        try {
          method = BaseNoGui.class.getMethod("getBuildFolder");
          File f = (File) method.invoke(null);
          return f.getAbsolutePath();
        } catch (SecurityException ex) {
          editor.statusError(ex);
        } catch (IllegalAccessException ex) {
          editor.statusError(ex);
        } catch (InvocationTargetException ex) {
          editor.statusError(ex);
        } catch (NoSuchMethodException ex) {
          editor.statusError(ex);
        }
      }
    }
    return "";
  }

  private long parseInt(String value){
    if(value.startsWith("0x")) return Long.parseLong(value.substring(2), 16);
    else return Integer.parseInt(value);
  }

  private long getIntPref(String name){
    String data = BaseNoGui.getBoardPreferences().get(name);
    if(data == null || data.contentEquals("")) return 0;
    return parseInt(data);
  }

  private void createAndErase(){
    if(!PreferencesData.get("target_platform").contentEquals("esp32")){
      System.err.println();
      editor.statusError("Erase Flash Not Supported on "+PreferencesData.get("target_platform"));
      return;
    }

    TargetPlatform platform = BaseNoGui.getTargetPlatform();

    String toolExtension = ".py";
    if(PreferencesData.get("runtime.os").contentEquals("windows")) {
      toolExtension = ".exe";
    } else if(PreferencesData.get("runtime.os").contentEquals("macosx")) {
      toolExtension = "";
    }

    String pythonCmd;
    if(PreferencesData.get("runtime.os").contentEquals("windows"))
        pythonCmd = "python.exe";
    else
        pythonCmd = "python";
    
    Boolean isNetwork = false;
    File espota = new File(platform.getFolder()+"/tools");
    File esptool = new File(platform.getFolder()+"/tools");
    String serialPort = PreferencesData.get("serial.port");

    //make sure the serial port or IP is defined
    if (serialPort == null || serialPort.isEmpty()) {
      System.err.println();
      editor.statusError("Erase Flash Error: serial port not defined!");
      return;
    }

    //find espota if IP else find esptool
    if(serialPort.split("\\.").length == 4){
      isNetwork = true;
      System.err.println();
      editor.statusError("Erase Flash Error: espota not supported!");
      return;
    } else {
      String esptoolCmd = "esptool"+toolExtension;
      esptool = new File(platform.getFolder()+"/tools", esptoolCmd);
      if(!esptool.exists() || !esptool.isFile()){
        esptool = new File(platform.getFolder()+"/tools/esptool_py", esptoolCmd);
        if(!esptool.exists()){
          esptool = new File(PreferencesData.get("runtime.tools.esptool_py.path"), esptoolCmd);
          if (!esptool.exists()) {
              System.err.println();
              editor.statusError("Erase Flash Error: esptool not found!");
              return;
          }
        }
      }
    }
    
    String sketchName = editor.getSketch().getName();
    String imagePath = getBuildFolderPath(editor.getSketch()) + "/" + sketchName + ".spiffs.bin";
    String uploadSpeed = BaseNoGui.getBoardPreferences().get("upload.speed");

    Object[] options = { "Yes", "No" };
    String title = "Erase Flash";
    String message = "Ready?";

    if(JOptionPane.showOptionDialog(editor, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]) != JOptionPane.YES_OPTION){
      System.err.println();
      editor.statusError("Erase Flash Warning: Erase Flash canceled!");
      return;
    }

    editor.statusNotice("Erase Flash...");

    if(esptool.getAbsolutePath().endsWith(".py"))
      sysExec(new String[]{pythonCmd, esptool.getAbsolutePath(), "--chip", "esp32", "--baud", uploadSpeed, "--port", serialPort, "erase_flash"});
    else
      sysExec(new String[]{esptool.getAbsolutePath(), "--chip", "esp32", "--baud", uploadSpeed, "--port", serialPort, "erase_flash"});
  }

  public void run() {
    createAndErase();
  }
}
