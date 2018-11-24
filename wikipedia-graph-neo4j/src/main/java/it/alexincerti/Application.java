
package it.alexincerti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import it.alexincerti.service.CalculateClosestMainCategory;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
//		WikiCategoryGraphDBCreator dbCreator = SpringApplication.run(Application.class, args)
//				.getBean(WikiCategoryGraphDBCreator.class);
//		dbCreator.create();
		CalculateClosestMainCategory calculateClosestMainCategory = SpringApplication.run(Application.class, args)
				.getBean(CalculateClosestMainCategory.class);
		calculateClosestMainCategory.calculateShortestDistancesToMainCategories(
				args[0],
				args[1], args[2],Integer.parseInt(args[3]), Long.parseLong(args[4]));
	}
}
