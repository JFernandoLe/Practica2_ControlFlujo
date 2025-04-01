import java.net.*;
import java.util.Map;
import java.util.TreeMap;
import java.io.*;
/**
 *
 * @author axele
 */
public class Servidor {
    public static void main(String[] args){
        try{
            int pto=1234;
            String msj="";
            DatagramSocket s = new DatagramSocket(pto);
            s.setReuseAddress(true);
            // s.setBroadcast(true);
            Integer contador=1,contadorInterno=1;
            Map<Integer, String> paquetesRecibidos = new TreeMap<>(); // Para almacenar los paquetes en desorden
            //Carpeta de almacenamiento
            File f=new File("");
            String ruta=f.getAbsolutePath();
            String carpeta="archivos";
            String ruta_archivos=ruta+"\\"+carpeta+"\\";
            System.out.println("Ruta: "+ruta_archivos);
            File f2=new File(ruta_archivos);
            f2.mkdirs();
            f2.setWritable(true);
            System.out.println("Servidor iniciado... espedando datagramas..");
            for(;;){
                byte[] b = new byte[65535];
                DatagramPacket p = new DatagramPacket(b,b.length);
                s.receive(p);
                // Convertir los datos en String, excluyendo el último byte (número de paquete)
                msj = new String(p.getData(), 0, p.getLength() - 1);
                // Leer el número de paquete, que es el último byte
                int numeroPaquete = p.getData()[p.getLength() - 1];
                if(numeroPaquete==contador){
                    System.out.println("Se recibió el paquete #"+numeroPaquete+" correctamente");
                    System.out.println("Se ha recibido datagrama desde "+p.getAddress()+":"+p.getPort()+" con el mensaje:"+msj);
                    contador++;
                    //vemos si el siguiente paquete está en el mapa
                    if(paquetesRecibidos.containsKey(contador)){
                        contadorInterno=contador; //Si esta lo procesamos
                        while(paquetesRecibidos.containsKey(contadorInterno)){
                            String msjRecuperado=paquetesRecibidos.get(contadorInterno);
                            System.out.println("Paquete #"+contadorInterno+" recuperado con el mensaje: "+msjRecuperado);
                            contadorInterno++;
                        }
                        contador=contadorInterno;
                    }
                }else{
                    System.out.println("Se recibió el paquete #"+numeroPaquete+" fuera de orden");
                    //Almacenamos el pquete para procesarlo más tarde
                    paquetesRecibidos.put(numeroPaquete,msj);
                }

                s.send(p);
            }//for
        }catch(Exception e){
            e.printStackTrace();
        }//catch

    }//main
}