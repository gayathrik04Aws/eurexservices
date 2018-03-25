package com.prisma.eurex.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DatesUtil {

	public static List<Date> getFolderDates(String folderPath){
		// TODO Auto-generated method stub
		Path dir = FileSystems.getDefault().getPath(folderPath);
		List<Date> dates = new ArrayList<Date>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
		        System.out.println(path.getFileName().toString());
		        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				try {
					Date folderdate = dateFormat.parse(path.getFileName().toString());
					dates.add(folderdate);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		Collections.reverse(dates);
		return dates;
	}

}
