import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GUIUredjaj1 extends JFrame implements ActionListener {
    private JPanel mainPanel;
    private JLabel label1;
    private JButton dugme;
    private JButton pokreniServerDugme;
    private JButton prekini;

    ServerSocket serverSocket;
    Thread captureThread;
    private volatile boolean videoIde = true;

    // Konstruktor klase GUIUredjaj1
    public GUIUredjaj1() {
        // Postavljanje osnovnih osobina prozora
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(500, 500);
        this.setContentPane(mainPanel);

        // Dodavanje listenera za dugmad
        dugme.addActionListener(this);
        prekini.addActionListener(this);
        pokreniServerDugme.addActionListener(this);

        this.setVisible(true);
    }

    public static void main(String[] args) {
        // Kreiranje instance klase
        new GUIUredjaj1();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Kada je pritisnuto dugme za pokretanje prenosa
        if (e.getSource() == dugme) {

            if (serverSocket == null) {
                label1.setText("Pokreni prvo server");
                return;
            }

            // Kreiranje niti za prenos slike i simulaciju klika mišem
            captureThread = new Thread(() -> {
                try {
                    dugme.setEnabled(false);
                    Socket klijentSocket = serverSocket.accept();
                    DataOutputStream dos = new DataOutputStream(klijentSocket.getOutputStream());
                    DataInputStream dis = new DataInputStream(klijentSocket.getInputStream());

                    System.out.println("Kreirani input i output streamovi na serveru");

                    new Thread(() -> {
                        try {
                            Robot robot = new Robot();
                            while (videoIde) {
                                int eventType = dis.readInt();

                                //ako se radi o kliku misa
                                if (eventType == 0) {
                                    int x = dis.readInt();
                                    int y = dis.readInt();
                                    int buttonType = dis.readInt();
                                    System.out.println("primljen klik");
                                    simulateMouseClick(x, y, buttonType);
                                }
                                //ako se radi o tastaturi
                                else if (eventType == 1) {
                                    int keyCode = dis.readInt();
                                    robot.keyPress(keyCode);
                                    robot.keyRelease(keyCode);

                                }
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        } catch (AWTException ex) {
                            throw new RuntimeException(ex);
                        }

                    }).start();

                    //Slanje dimenzija servera
                    Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                    int width = size.width;
                    int height = size.height;

                    dos.writeInt(width);
                    dos.writeInt(height);
                    dos.flush();

                    // Slanje slike klijentu
                    while (videoIde) {
                        System.out.println("Salje se slika klijentu!");
                        byte[] niz = pretvoriSlikuUBajtove(napraviScreenshot());
                        dos.writeInt(niz.length);  // šalje se dužina niza sa slikom
                        dos.write(niz);            // šalje se slika
                        dos.flush();

                        Thread.sleep(50);
                    }
                    dos.close();
                    klijentSocket.close();
                    dugme.setEnabled(true);
                    videoIde = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            captureThread.start();
        }

        // Za pokretanje servera
        if (e.getSource() == pokreniServerDugme) {
            try {
                serverSocket = new ServerSocket(12345);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            pokreniServerDugme.setText("Server je pokrenut");
            pokreniServerDugme.setEnabled(false);
        }
        // Za prekidanje prenosa
        if (e.getSource() == prekini) {

            videoIde = false;
        }
    }

    // Simulacija klika mišem na određenim koordinatama
    private void simulateMouseClick(int x, int y, int buttonType) {
        try {
            Robot robot = new Robot();
            robot.mouseMove(x, y);

            int buttonDownMask = 0;
            switch (buttonType) {
                case MouseEvent.BUTTON1:  // Levo dugme miša
                    buttonDownMask = InputEvent.BUTTON1_DOWN_MASK;
                    break;
                case MouseEvent.BUTTON2:  // Srednje dugme miša
                    buttonDownMask = InputEvent.BUTTON2_DOWN_MASK;
                    break;
                case MouseEvent.BUTTON3:  // Desno dugme miša
                    buttonDownMask = InputEvent.BUTTON3_DOWN_MASK;
                    break;
            }

            System.out.println("Simuliran klik");

            robot.mousePress(buttonDownMask);
            robot.mouseRelease(buttonDownMask);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // Pretvaranje BufferedImage u niz bajtova
    private byte[] pretvoriSlikuUBajtove(BufferedImage slika) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(slika, "png", baos);
        } catch (IOException e) {
            System.out.println("greksa prilikom pretvaranja slike u bajtove");
        }
        byte[] imageData = baos.toByteArray();
        return imageData;
    }

    // Pravljenje screenshot-a trenutnog ekrana
    private BufferedImage napraviScreenshot() {
        Robot robot = null;
        BufferedImage screenshot;
        try {
            robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            screenshot = robot.createScreenCapture(screenRect);
            return screenshot;
        } catch (AWTException e) {
            System.out.println("Greska prilikom pravljenja screenshot-a.");
        }
        return null;
    }

}