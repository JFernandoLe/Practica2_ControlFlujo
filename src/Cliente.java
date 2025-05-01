import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JFileChooser;

public class Cliente {
    private static final int WINDOW_SIZE = 5;

    public static void main(String[] args) {
        try {
            int pto = 8000;
            String dir = "127.0.0.1";
            DatagramSocket cl = new DatagramSocket();
            System.out.println("Conexión establecida.. Escoja un archivo para enviar...");
            JFileChooser jf = new JFileChooser();
            int r = jf.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) {
                File f = jf.getSelectedFile();
                String nombre = f.getName();
                String path = f.getAbsolutePath();
                long tam = f.length();
                System.out.println("Preparándose para enviar archivo " + path + " de " + tam + " bytes\n\n");

                TreeMap<Integer, byte[]> copiaPaquetes = new TreeMap<>();
                TreeMap<Integer, Long> tiemposEnvio = new TreeMap<>();
                Map<Integer, byte[]> pendientesFueraDeOrden = new TreeMap<>();

                byte[] nombreBytes = nombre.getBytes();
                DatagramPacket p = new DatagramPacket(nombreBytes, nombreBytes.length, InetAddress.getByName(dir), pto);
                cl.send(p);
                byte[] tamBytes = String.valueOf(tam).getBytes();
                p = new DatagramPacket(tamBytes, tamBytes.length, InetAddress.getByName(dir), pto);
                cl.send(p);

                DataInputStream dis = new DataInputStream(new FileInputStream(path));
                long enviados = 0;
                int l = 0, base = 0, numSeqSig = 0;
                byte[][] window = new byte[WINDOW_SIZE][];

                cl.setSoTimeout(500);

                while (enviados < tam) {
                    while (numSeqSig < base + WINDOW_SIZE && enviados < tam) {
                        byte[] b = new byte[1500];
                        l = dis.read(b);
                        if (l == -1) break;

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeInt(numSeqSig);
                        dos.write(b, 0, l);
                        byte[] packetData = baos.toByteArray();

                        // 20% probabilidad de posponer el envío (simular fuera de orden)
                        if (Math.random() < 0.2) {
                            pendientesFueraDeOrden.put(numSeqSig, packetData);
                            copiaPaquetes.put(numSeqSig, packetData);
                            tiemposEnvio.put(numSeqSig, System.currentTimeMillis());
                            System.out.println("Paquete pospuesto (fuera de orden): " + numSeqSig);
                        } else {
                            p = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(dir), pto);
                            cl.send(p);
                            if (Math.random() < 0.3) {
                                cl.send(p);
                                System.out.println("Paquete duplicado enviado: " + numSeqSig);
                            }
                            copiaPaquetes.put(numSeqSig, packetData);
                            tiemposEnvio.put(numSeqSig, System.currentTimeMillis());
                            System.out.println("Paquete enviado: " + numSeqSig);
                        }

                        enviados += l;
                        numSeqSig++;
                    }

                    // Enviar los paquetes fuera de orden al final de cada ventana
                    for (Map.Entry<Integer, byte[]> entry : pendientesFueraDeOrden.entrySet()) {
                        int seq = entry.getKey();
                        byte[] datos = entry.getValue();
                        DatagramPacket fueraOrden = new DatagramPacket(datos, datos.length, InetAddress.getByName(dir), pto);
                        cl.send(fueraOrden);
                        tiemposEnvio.put(seq, System.currentTimeMillis());
                        System.out.println("Enviado fuera de orden: " + seq);
                    }
                    pendientesFueraDeOrden.clear();

                    byte[] ack = new byte[4];
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                    try {
                        cl.receive(ackPacket);
                        int ackNum = new DataInputStream(new ByteArrayInputStream(ack)).readInt();
                        System.out.println("ACK recibido: " + ackNum);
                        copiaPaquetes.remove(ackNum);
                        tiemposEnvio.remove(ackNum);
                        if (ackNum == base) {
                            while (!copiaPaquetes.containsKey(base) && base < numSeqSig) {
                                base++;
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout detectado. Reenviando pendientes...");
                        long ahora = System.currentTimeMillis();
                        for (Map.Entry<Integer, byte[]> entry : copiaPaquetes.entrySet()) {
                            int numSeq = entry.getKey();
                            long tEnvio = tiemposEnvio.get(numSeq);
                            if (ahora - tEnvio > 500) {
                                byte[] datos = entry.getValue();
                                DatagramPacket pReenvio = new DatagramPacket(datos, datos.length, InetAddress.getByName(dir), pto);
                                cl.send(pReenvio);
                                tiemposEnvio.put(numSeq, ahora);
                                System.out.println("Reenviado paquete: " + numSeq);
                            }
                        }
                    }
                }

                // FIN (-1)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dosFin = new DataOutputStream(baos);
                dosFin.writeInt(-1);
                byte[] finBytes = baos.toByteArray();
                DatagramPacket finPacket = new DatagramPacket(finBytes, finBytes.length, InetAddress.getByName(dir), pto);
                cl.send(finPacket);
                System.out.println("FIN de transmisión enviado");

                System.out.println("\nArchivo enviado correctamente.");
                dis.close();
                cl.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
