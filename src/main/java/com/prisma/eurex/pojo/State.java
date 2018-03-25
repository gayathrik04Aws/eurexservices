package com.prisma.eurex.pojo;

import java.io.Serializable;
import java.util.Date;

public class State implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getDatasetDate() {
		return datasetDate;
	}

	public void setDatasetDate(Date datasetDate) {
		this.datasetDate = datasetDate;
	}

	private String status;
	
	private Date datasetDate;

}
