package com.piyush.lodgy.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.stereotype.Component;

@Component
@Entity
public class Lodge {
	@Id
	@GeneratedValue
	private String lodgeId;
	
	@Column(nullable = false)
	private String name;

	public String getLodgeId() {
		return lodgeId;
	}

	public void setLodgeId(String lodgeId) {
		this.lodgeId = lodgeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
