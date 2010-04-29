package com.izforge.izpack.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sora
 * Date: Nov 23, 2009
 * Time: 11:37:06 PM
 * To change this template use File | Settings | File Templates.
 */
public final class Console
{

    public static final int INITIAL_WIDTH = 800;

    public static final int INITIAL_HEIGHT = 600;

    public static void main(String[] args)
    {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try
        {

            /*
             * Start a new process in which to execute the commands in cmd, using the environment in
             * env and use pwd as the current working directory.
             */
            process = runtime.exec(args);// , env, pwd);
            new Console(process);
            System.exit(process.exitValue());
        }
        catch (IOException e)
        {
            /*
             * Couldn't even get the command to start. Most likely it couldn't be found because of a
             * typo.
             */
            System.out.println("Error starting: " + args[0]);
            System.out.println(e);
        }
    }

    private StdOut so;

    private StdOut se;

    public String getOutputData()
    {
        if (so != null)
        {
            return so.getData();
        }
        else
        {
            return "";
        }
    }

    public String getErrorData()
    {
        if (se != null)
        {
            return se.getData();
        }
        else
        {
            return "";
        }
    }

    public Console(final Process p)
    {
        JFrame frame = new JFrame();
        frame.setTitle("Console");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width / 2 - INITIAL_WIDTH / 2, screenSize.height / 2
                - INITIAL_HEIGHT / 2);
        ConsoleTextArea cta = new ConsoleTextArea();
        JScrollPane scroll = new JScrollPane(cta);
        scroll.setPreferredSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));
        frame.getContentPane().add(scroll);
        frame.pack();
        frame.addWindowListener(new WindowListener()
        {

            public void windowActivated(WindowEvent e)
            {
            }

            public void windowClosed(WindowEvent e)
            {
            }

            public void windowClosing(WindowEvent e)
            {
                p.destroy();
            }

            public void windowDeactivated(WindowEvent e)
            {
            }

            public void windowDeiconified(WindowEvent e)
            {
            }

            public void windowIconified(WindowEvent e)
            {
            }

            public void windowOpened(WindowEvent e)
            {
            }
        });

        // From here down your shell should be pretty much
        // as it is written here!
        /*
         * Start up StdOut, StdIn and StdErr threads that write the output generated by the process
         * p to the screen, and feed the keyboard input into p.
         */
        so = new StdOut(p, cta);
        se = new StdOut(p, cta);
        StdIn si = new StdIn(p, cta);
        so.start();
        se.start();
        si.start();

        // Wait for the process p to complete.
        try
        {
            frame.setVisible(true);
            p.waitFor();
        }
        catch (InterruptedException e)
        {
            /*
             * Something bad happened while the command was executing.
             */
            System.out.println("Error during execution");
            System.out.println(e);
        }

        /*
         * Now signal the StdOut, StdErr and StdIn threads that the process is done, and wait for
         * them to complete.
         */
        try
        {
            so.done();
            se.done();
            si.done();
            so.join();
            se.join();
            si.join();
        }
        catch (InterruptedException e)
        {
            // Something bad happend to one of the Std threads.
            System.out.println("Error in StdOut, StdErr or StdIn.");
            System.out.println(e);
        }
        frame.setVisible(false);
    }
}


class ConsoleTextArea extends JTextArea implements KeyListener, DocumentListener
{

    /**
     *
     */
    private static final long serialVersionUID = 3258410625414475827L;

    private ConsoleWriter console1;

    private PrintStream out;

    private PrintStream err;

    private PrintWriter inPipe;

    private PipedInputStream in;

    private List<String> history;

    private int historyIndex = -1;

    private int outputMark = 0;

    public void select(int start, int end)
    {
        requestFocus();
        super.select(start, end);
    }

    public ConsoleTextArea()
    {
        super();
        history = new ArrayList<String>();
        console1 = new ConsoleWriter(this);
        ConsoleWriter console2 = new ConsoleWriter(this);
        out = new PrintStream(console1);
        err = new PrintStream(console2);
        PipedOutputStream outPipe = new PipedOutputStream();
        inPipe = new PrintWriter(outPipe);
        in = new PipedInputStream();
        try
        {
            outPipe.connect(in);
        }
        catch (IOException exc)
        {
            exc.printStackTrace();
        }
        getDocument().addDocumentListener(this);
        addKeyListener(this);
        setLineWrap(true);
        setFont(new Font("Monospaced", 0, 12));
    }

    void returnPressed()
    {
        Document doc = getDocument();
        int len = doc.getLength();
        Segment segment = new Segment();
        try
        {
            synchronized (doc)
            {
                doc.getText(outputMark, len - outputMark, segment);
            }
        }
        catch (javax.swing.text.BadLocationException ignored)
        {
            ignored.printStackTrace();
        }
        if (segment.count > 0)
        {
            history.add(segment.toString());
        }
        historyIndex = history.size();
        inPipe.write(segment.array, segment.offset, segment.count);
        append("\n");
        synchronized (doc)
        {
            outputMark = doc.getLength();
        }
        inPipe.write("\n");
        inPipe.flush();
        console1.flush();
    }

    public void eval(String str)
    {
        inPipe.write(str);
        inPipe.write("\n");
        inPipe.flush();
        console1.flush();
    }

    public void keyPressed(KeyEvent e)
    {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_LEFT)
        {
            if (outputMark == getCaretPosition())
            {
                e.consume();
            }
        }
        else if (code == KeyEvent.VK_HOME)
        {
            int caretPos = getCaretPosition();
            if (caretPos == outputMark)
            {
                e.consume();
            }
            else if (caretPos > outputMark)
            {
                if (!e.isControlDown())
                {
                    if (e.isShiftDown())
                    {
                        moveCaretPosition(outputMark);
                    }
                    else
                    {
                        setCaretPosition(outputMark);
                    }
                    e.consume();
                }
            }
        }
        else if (code == KeyEvent.VK_ENTER)
        {
            returnPressed();
            e.consume();
        }
        else if (code == KeyEvent.VK_UP)
        {
            historyIndex--;
            if (historyIndex >= 0)
            {
                if (historyIndex >= history.size())
                {
                    historyIndex = history.size() - 1;
                }
                if (historyIndex >= 0)
                {
                    String str = history.get(historyIndex);
                    int len = getDocument().getLength();
                    replaceRange(str, outputMark, len);
                    int caretPos = outputMark + str.length();
                    select(caretPos, caretPos);
                }
                else
                {
                    historyIndex++;
                }
            }
            else
            {
                historyIndex++;
            }
            e.consume();
        }
        else if (code == KeyEvent.VK_DOWN)
        {
            int caretPos = outputMark;
            if (history.size() > 0)
            {
                historyIndex++;
                if (historyIndex < 0)
                {
                    historyIndex = 0;
                }
                int len = getDocument().getLength();
                if (historyIndex < history.size())
                {
                    String str = history.get(historyIndex);
                    replaceRange(str, outputMark, len);
                    caretPos = outputMark + str.length();
                }
                else
                {
                    historyIndex = history.size();
                    replaceRange("", outputMark, len);
                }
            }
            select(caretPos, caretPos);
            e.consume();
        }
    }

    public void keyTyped(KeyEvent e)
    {
        int keyChar = e.getKeyChar();
        if (keyChar == 0x8 /* KeyEvent.VK_BACK_SPACE */)
        {
            if (outputMark == getCaretPosition())
            {
                e.consume();
            }
        }
        else if (getCaretPosition() < outputMark)
        {
            setCaretPosition(outputMark);
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public synchronized void write(String str)
    {
        insert(str, outputMark);
        int len = str.length();
        outputMark += len;
        select(outputMark, outputMark);
    }

    public synchronized void insertUpdate(DocumentEvent e)
    {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off)
        {
            outputMark += len;
        }
    }

    public synchronized void removeUpdate(DocumentEvent e)
    {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off)
        {
            if (outputMark >= off + len)
            {
                outputMark -= len;
            }
            else
            {
                outputMark = off;
            }
        }
    }

    public void postUpdateUI()
    {
        // this attempts to cleanup the damage done by updateComponentTreeUI
        requestFocus();
        setCaret(getCaret());
        synchronized (this)
        {
            select(outputMark, outputMark);
        }
    }

    public void changedUpdate(DocumentEvent e)
    {
    }

    public InputStream getIn()
    {
        return in;
    }

    public PrintStream getOut()
    {
        return out;
    }

    public PrintStream getErr()
    {
        return err;
    }

}