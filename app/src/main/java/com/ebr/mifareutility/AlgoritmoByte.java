package com.ebr.mifareutility;

import java.util.Locale;

/**
 * Created by Elias y apoyo de Richardo Industries on 12/11/14.
 */


public class AlgoritmoByte {



    public static void main (String[] args) {
        int valor = 789;
        byte direccion = 5;

        int valorn = (int)~valor;
        byte direccionn = (byte)~direccion;


        String valorS = formato(limita(Integer.toHexString(valor)));
        String valornS = formato(limita(Integer.toHexString(valorn)));
        String direccionS = limita2(Integer.toHexString(direccion));
        String direccionNS = limita2(Integer.toHexString(direccionn));





        System.out.println(valorS);

        System.out.println(formato(valorS));
        System.out.println(limita(Integer.toHexString(valorn)));
        System.out.println(Integer.toHexString(direccion));
        System.out.println(Integer.toHexString(direccionn));
        //System.out.println(valorStringn);

        String buena=valorS+valornS+valorS+direccionS+direccionNS+direccionS+direccionNS;


        System.out.println(buena);


    }


    public static String formato ( String valor){

        String buena="";

        for (int i=valor.length();i>=2;i-=2){

            buena +=valor.substring(i-2,i);



        }

        return buena;


    }

    public static String getHexString(byte[] b, int length)
    {
        String result = "";
        Locale loc = Locale.getDefault();

        for (int i = 0; i < length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += " ";
        }
        return result.toUpperCase(loc);
    }


    public static byte[] magia(int valor, byte direccion ){
        int valorn = (int)~valor;
        byte direccionn = (byte)~direccion;
        String valorS = formato(limita(Integer.toHexString(valor)));
        String valornS = formato(limita(Integer.toHexString(valorn)));
        String direccionS = limita2(Integer.toHexString(direccion));
        String direccionNS = limita2(Integer.toHexString(direccionn));

        String buena=valorS+valornS+valorS+direccionS+direccionNS+direccionS+direccionNS;
        byte[] buenaByte = hexStringToByteArray(buena);
        return buenaByte;

    }



    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String limita(String valor){

        int max = 8;
        String regreso= "" ;
        for(int i=0; i<(8-valor.length());i++){

            regreso+= "0";

        }

        return regreso+valor;
    }

    public static String limita2(String valor){

        int max = 2;
        String regreso= "" ;
        if (valor.length()<=2){
            for(int i=0; i<(2-valor.length());i++){

                regreso+= "0";
            }
        }else{



            return valor.substring(6,8);

        }

        return regreso+valor;
    }




    public static String reverse(String input){
        char[] in = input.toCharArray();
        int begin=0;
        int end=in.length-1;
        char temp;
        while(end>begin){
            temp = in[begin];
            in[begin]=in[end];
            in[end] = temp;
            end--;
            begin++;
        }
        return new String(in);
    }
}
