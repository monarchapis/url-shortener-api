package com.monarchapis.api.urlshortener.v1.service.mongodb;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.monarchapis.api.urlshortener.v1.model.ShortenedUrl;
import com.monarchapis.api.urlshortener.v1.service.UrlShortenerService;
import com.monarchapis.driver.exception.ConflictException;
import com.monarchapis.driver.model.Claims;
import com.monarchapis.driver.model.ClaimNames;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class MongoDBUrlShortenerService implements UrlShortenerService {
	private MongoClient mongo;
	private DB db;
	private DBCollection collection;

	public MongoDBUrlShortenerService(String host, int port, String database) throws UnknownHostException {
		mongo = new MongoClient(host, port);
		mongo.setWriteConcern(WriteConcern.JOURNALED);
		db = mongo.getDB(database);
		collection = db.getCollection("urls");

		collection.createIndex( //
				new BasicDBObject("longUrl", 1), //
				new BasicDBObject("name", "by-longUrl")); //

		collection.createIndex( //
				new BasicDBObject("userId", 1), //
				new BasicDBObject("name", "by-userId"));
	}

	@PreDestroy
	public void close() {
		mongo.close();
	}

	@Override
	public ShortenedUrl shorten(String longUrl) {
		ShortenedUrl url = null;
		DBObject o = collection.findOne( //
				new BasicDBObject("longUrl", longUrl));

		if (o != null) {
			url = convert(o);
		} else {
			String slug = RandomStringUtils.randomAlphanumeric(6);
			url = create(longUrl, slug);
		}

		return url;
	}

	@Override
	public ShortenedUrl shorten(String longUrl, String slug) {
		DBObject o = collection.findOne( //
				new BasicDBObject("_id", slug));

		if (o != null) {
			throw new ConflictException("shortenedUrls", slug);
		}

		return create(longUrl, slug);
	}

	private ShortenedUrl create(String longUrl, String slug) {
		String userId = Claims.current().getSubject().or("unknown");

		DBObject doc = new BasicDBObject("_id", slug) //
				.append("longUrl", longUrl) //
				.append("visits", 0L) //
				.append("createdBy", userId) //
				.append("createdDate", new Date());

		collection.insert(doc);

		return convert(doc);
	}

	public Optional<ShortenedUrl> expand(String slug) {
		DBObject o = collection.findOne(new BasicDBObject("_id", slug));

		return Optional.fromNullable(o != null ? convert(o) : null);
	}

	public void signalVisit(String slug) {
		collection.update( //
				new BasicDBObject("_id", slug), //
				new BasicDBObject("$inc", new BasicDBObject("visits", 1)));
	}

	public long getVisits(String slug) {
		DBObject o = collection.findOne( //
				new BasicDBObject("_id", slug), //
				new BasicDBObject("visits", 1));

		return (o != null) ? ((Number) o.get("visits")).longValue() : 0;
	}

	private ShortenedUrl convert(DBObject o) {
		ShortenedUrl url = new ShortenedUrl();

		url.setSlug(o.get("_id").toString());
		url.setLongUrl((String) o.get("longUrl"));
		url.setVisits(((Number) o.get("visits")).longValue());
		url.setCreatedBy((String) o.get("createdBy"));
		url.setCreatedDate(new DateTime((Date) o.get("createdDate")));

		return url;
	}

	@Override
	public List<ShortenedUrl> urlsByUserId(String userId) {
		List<ShortenedUrl> urls = new LinkedList<ShortenedUrl>();
		DBCursor cursor = collection.find(new BasicDBObject("createdBy", userId));

		while (cursor.hasNext()) {
			DBObject o = cursor.next();
			urls.add(convert(o));
		}

		return urls;
	}
}
