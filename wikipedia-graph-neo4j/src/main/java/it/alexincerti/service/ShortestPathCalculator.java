package it.alexincerti.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.alexincerti.models.Category;
import it.alexincerti.repository.CategoryRepository;
import it.alexincerti.repository.CategoryRepositoryEmbedded;

@Service
public class ShortestPathCalculator {
	Logger logger = LoggerFactory.getLogger(ShortestPathCalculator.class);

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private CategoryRepositoryEmbedded categoryRepositoryEmbedded;

	public CategoryPath getClosestNode(String startingNode, List<String> endNodes) {
		List<Pair<String, String>> pairs = new ArrayList<>();
		for (String endNode : endNodes) {
			if (startingNode.equals(endNode)) {
				CategoryPath categoryPath = new CategoryPath();
				categoryPath.setEndCategory(endNode);
				categoryPath.setStartCategory(startingNode);
				categoryPath.setLength((long) 0);
				return categoryPath;
			}
			pairs.add(Pair.of(startingNode, endNode));
		}
		// Long pairDistance = getShortestPathLength(pairs);
//		if (pairDistance != null && (pairDistance.getLength() != null) && //
//				(closestPair == null || (closestPair.getLength() == null)
//						|| (pairDistance.getLength() < closestPair.getLength()))) {
//			closestPair = pairDistance;
//		}

		return getShortestPathInBatch(pairs);
	}

	public CategoryPath getShortestPath(String startingCategoryNode, String endCategoryNode) {
		CategoryPath path = new CategoryPath();

		if (startingCategoryNode == null || endCategoryNode == null) {
			return null;
		}
		try {
			Category startCategory = getCategoryRepository().findByName(startingCategoryNode);
			Category endCategory = getCategoryRepository().findByName(endCategoryNode);
			//
			if (startCategory == null || endCategory == null) {
				return null;
			}
			path.setStartCategory(startingCategoryNode);
			path.setEndCategory(endCategoryNode);
			//
			// Long shortestPath = getShortestPathLength(startingCategoryNode,
			// endCategoryNode);
			// Long shortestPath =
			// getCategoryRepository().getShortestPathLength(startingCategoryNode,
			// endCategoryNode);
			// path.setLength(shortestPath);
			return path;
		} catch (

		Exception e) {
			e.printStackTrace();
			System.err.println(
					String.format("Something went wrong calculating distances. starting node |%s| and end node |%s|",
							startingCategoryNode, endCategoryNode));
			return null;
		}
	}

	private CategoryPath getShortestPathInBatch(List<Pair<String, String>> startEndNodes) {
		String query = "MATCH p=shortestPath((a:Category {name: $startNode})-[*]->(b:Category {name: $endNode})) RETURN a.name, b.name, length(p)";
		StringBuilder finalQuery = new StringBuilder();
		Session session = getSessionFactory().openSession();
		final AtomicInteger index = new AtomicInteger(0);
		HashMap<String, Object> parameters = new HashMap<>();
		startEndNodes.forEach(pair -> {
			if (index.getAndIncrement() != 0) {
				finalQuery.append(" UNION ");
			}
			finalQuery.append(query.replace("startNode", "startNode" + index).replace("endNode", "endNode" + index));
			parameters.put("startNode" + index, pair.getLeft());
			parameters.put("endNode" + index, pair.getRight());
		});
		Result result = session.query(finalQuery.toString(), parameters);
		Iterable<Map<String, Object>> iterable = () -> result.iterator();
		Stream<Map<String, Object>> targetStream = StreamSupport.stream(iterable.spliterator(), false);
		Map<String, Object> path = targetStream
				.min((a, b) -> ((Integer) Integer.parseInt(a.get("length(p)").toString()))
						.compareTo((Integer) Integer.parseInt(b.get("length(p)").toString())))
				.orElseGet(() -> null);

		if (path == null) {
			return null;
		}

		CategoryPath categoryPath = new CategoryPath();
		categoryPath.setStartCategory(path.get("a.name").toString());
		categoryPath.setEndCategory(path.get("b.name").toString());
		categoryPath.setLength(Long.parseLong(path.get("length(p)").toString()));
		return categoryPath;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	// public CategoryPath getShortestPathEmbedded(String startingCategoryNode,
	// String endCategoryNode) {
//		CategoryPath path = new CategoryPath();
//
//		if (startingCategoryNode == null || endCategoryNode == null) {
//			return null;
//		}
//		try {
//			path.setStartCategory(new Category(startingCategoryNode));
//			path.setEndCategory(new Category(endCategoryNode));
//			//
//			List<Category> shortestPath = getCategoryRepositoryEmbedded().getShortestPath(startingCategoryNode,
//					endCategoryNode);
//			path.setNodes(shortestPath);
//			return path;
//		} catch (
//
//		Exception e) {
//			e.printStackTrace();
//			System.err.println(
//					String.format("Something went wrong calculating distances. starting node |%s| and end node |%s|",
//							startingCategoryNode, endCategoryNode));
//			return null;
//		}
	// }

	public CategoryRepositoryEmbedded getCategoryRepositoryEmbedded() {
		return categoryRepositoryEmbedded;
	}

	public CategoryRepository getCategoryRepository() {
		return categoryRepository;
	}
}