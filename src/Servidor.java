import java.net.*;
import java.sql.SQLOutput;
import java.util.Map;
import java.util.TreeMap;
import java.io.*;

public class Servidor {
    public static void main(String[] args) {
        try {
            int pto = 8000;
            DatagramSocket s = new DatagramSocket(pto);
            System.out.println("Servidor UDP esperando archivos...");
            byte[] buffer = new byte[1500];
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            s.receive(p); //Recibimos el paquete con el nombre del archivo
            String nombre = new String(p.getData(), 0, p.getLength()); //Obtenemos el nombre del archivo, viene en el paquete
            s.receive(p); //Recibimos el paquete con el tamaño del archivo
            long tam = Long.parseLong(new String(p.getData(), 0, p.getLength())); //El tamaño viene como String, lo pasamos a entero
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(nombre)); //Se crea un flujo de salida para guardar el archivo con el nombre recibido.
            long recibidos = 0;
            int l, porcentaje = 0, numSeqEsp = 0;

            while (recibidos < tam) {
                s.receive(p); //Vamos recibiendo los paquetes que contienen el archivo
                byte[] data = p.getData(); //Guardamos el paquete en un arreglo de bytes

                int numSeq = new DataInputStream(new ByteArrayInputStream(data)).readInt(); //Creamos un flujo de entrada que contiene el numero de secuencia enviado por el cliente en el paquete

                if (numSeq == numSeqEsp) {
                    dos.write(data, 4, p.getLength() - 4); //Escribe los datos (excluyendo los primeros 4 bytes, que corresponden al número de secuencia).
                    recibidos += p.getLength() - 4;
                    porcentaje = (int) ((recibidos * 100) / tam);
                    System.out.print("\rRecibido el " + porcentaje + " % del archivo");
                    numSeqEsp++;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dosAck = new DataOutputStream(baos);
                dosAck.writeInt(numSeq); //Escribimos el numero de secuencia actual como acuse en el flujo
                byte[] ack = baos.toByteArray();
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, p.getAddress(), p.getPort());
                s.send(ackPacket); //Enviamos el acuse
                System.out.println("ACK enviado: " + numSeq);
            }

            System.out.println("\nArchivo recibido correctamente.");
            dos.close();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
