package com.hyunnyapp.brainyproject.brainycontroler;

import android.util.Log;

public class Utils 
{
    private final static String TAG = "Utils";

    public static byte[] intToByteArray(int a) 
    {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int byteArrayToInt(byte[] b) 
    {
        return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
    }

    public static int ckeckHeader(byte[] in, byte[] sequence) 
    {
        int seqIndex = 0;
        byte c;
        
        for(int i=0; i < in.length; i++) 
        {
            c = (byte) in[i];
            
            //if(Constants.D) Log.i(TAG,"in["+i+"]="+ c);
            
            if(c == sequence[seqIndex]) 
            {
                if(Constants.UIDEBUG) Log.i(TAG,"c == sequence["+ seqIndex +"]");
                
                seqIndex++;
                if(seqIndex == sequence.length)
                {
                    if(Constants.UIDEBUG) Log.i(TAG,"ckeckHeader return: "+ (i + 1 - sequence.length));
                	return i + 1 - sequence.length;
                }
            } 
            else 
            	seqIndex = 0;
        }
        return -1;
    }

    public static int BytesIndexOf(byte[] Source, byte[] Search, int fromIndex) 
    {
    	  boolean Find = false;
    	  int i;
    	  
    	  for (i = fromIndex;i<Source.length-Search.length;i++)
    	  {
    	    if(Source[i]==Search[0])
    	    {
    	      Find = true;
    	      for (int j = 0;j<Search.length;j++)
    	      {
    	        if (Source[i+j]!=Search[j])
    	        {
    	          Find = false;
    	        }
    	      }
    	    }
    	    
    	    if(Find)
    	    {
    	      break;
    	    }
    	  }
    	  if(!Find)
    	  {
    	    return -1;
    	  }
    	  return  i;
    	}
}