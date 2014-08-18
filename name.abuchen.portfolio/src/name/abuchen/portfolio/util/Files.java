package name.abuchen.portfolio.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Files
{

    public static List<String> readAllLines(File file, Charset charset) throws IOException
    {
        List<String> answer = new ArrayList<String>();

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset.name()));
            String line = reader.readLine();
            while (line != null)
            {
                answer.add(line);
                line = reader.readLine();
            }
        }
        finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            }
            catch (IOException ignore)
            {}
        }

        return answer;
    }

    public static void write(File file, List<String> lines, Charset charset) throws IOException
    {
        PrintWriter writer = null;

        try
        {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), charset.name()));

            for (String line : lines)
                writer.println(line);
        }
        finally
        {
            if (writer != null)
                writer.close();
        }
    }

    public static byte[] readAllBytes(File file) throws IOException
    {
        InputStream input = null;
        try
        {
            input = new BufferedInputStream(new FileInputStream(file));

            int size = (int) file.length();

            byte[] data = new byte[size];
            int offset = 0;
            int readed;

            while (offset < size && (readed = input.read(data, offset, size - offset)) != -1)
            {
                offset += readed;
            }

            return data;
        }
        finally
        {
            try
            {
                if (input != null)
                    input.close();
            }
            catch (IOException ignore)
            {}
        }
    }

}
