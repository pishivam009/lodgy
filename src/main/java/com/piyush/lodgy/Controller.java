package com.piyush.lodgy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.piyush.lodgy.data.Lodge;

@RestController
public class Controller {

	@Autowired
	Lodge lodge;

}
