import java.net.*;
import java.util.*;
import java.io.*;

public class Servidor {
    public static void main(String[] args) {
        try {
            int pto = 8000;
            boolean fin = false;
            DatagramSocket s = new DatagramSocket(pto);
            System.out.println("Servidor UDP esperando archivos...");
            byte[] buffer = new byte[1500];
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            Set<Integer> paquetesRecibidos = new HashSet<>();
            TreeMap<Integer, byte[]> bufferFueraDeOrden = new TreeMap<>();

            s.receive(p); // Nombre del archivo
            String nombre = new String(p.getData(), 0, p.getLength());
            s.receive(p); // Tamaño del archivo
            long tam = Long.parseLong(new String(p.getData(), 0, p.getLength()));

            DataOutputStream dos = new DataOutputStream(new FileOutputStream(nombre));
            long recibidos = 0;
            int porcentaje = 0, numSeqEsp = 0;

            while (!fin) {
                s.receive(p);
                byte[] data = p.getData();
                int numSeq = new DataInputStream(new ByteArrayInputStream(data)).readInt();

                if (numSeq == -1) {
                    System.out.println("\nFIN recibido. Cerrando servidor.");
                    fin = true;
                    break;
                }

                if (paquetesRecibidos.contains(numSeq)) {
                    System.out.println("Paquete duplicado detectado: " + numSeq);
                } else {
                    paquetesRecibidos.add(numSeq);
                    byte[] datosReales = Arrays.copyOfRange(data, 4, p.getLength());
                    bufferFueraDeOrden.put(numSeq, datosReales);
                }

                // ACK siempre
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dosAck = new DataOutputStream(baos);
                dosAck.writeInt(numSeq);
                byte[] ack = baos.toByteArray();
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, p.getAddress(), p.getPort());

                if (Math.random() > 0.2) {
                    s.send(ackPacket);
                    System.out.println("ACK enviado: " + numSeq);
                } else {
                    System.out.println("ACK perdido: " + numSeq);
                }

                // Escribir en orden (flush)
                while (bufferFueraDeOrden.containsKey(numSeqEsp)) {
                    byte[] chunk = bufferFueraDeOrden.remove(numSeqEsp);
                    dos.write(chunk);
                    recibidos += chunk.length;
                    porcentaje = (int) ((recibidos * 100) / tam);

                    if (numSeqEsp != numSeq) {
                        System.out.println("Escrito paquete fuera de orden desde buffer: " + numSeqEsp);
                    } else {
                        System.out.print("\rAvance: " + porcentaje + "% del archivo");
                    }

                    numSeqEsp++;
                }
            }

            System.out.println("\n------------------------------------");
            System.out.println("Paquetes recibidos: " + paquetesRecibidos);
            System.out.println("Archivo recibido correctamente.");

            dos.close();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
