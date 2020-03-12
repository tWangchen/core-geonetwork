package org.fao.geonet.kernel.aws;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3Operation {

	private List<S3ObjectSummary> getBucketObjectSummaries(String url) throws Exception {

		List<S3ObjectSummary> s3ObjectSummaries = new ArrayList<S3ObjectSummary>();
		AmazonS3URI s3uri = new AmazonS3URI(url);
		AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(s3uri.getRegion()).build();
		String bucketname = s3uri.getBucket();
		try {
			
			int index = url.indexOf(bucketname);
			String prefix = url.substring(index + bucketname.length());
			if(prefix.length() > 0 && prefix.startsWith("/")){
				prefix = prefix.substring(1, prefix.length());
			}
			if(prefix.length() > 0 && !prefix.endsWith("/")){
				prefix = prefix + "/";
			}
			
			ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketname)
					.withPrefix(prefix.trim()).withDelimiter("/");
			ObjectListing objectListing;

			do {
				objectListing = s3client.listObjects(listObjectsRequest);
				for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
					s3ObjectSummaries.add(objectSummary);
				}
				listObjectsRequest.setMarker(objectListing.getNextMarker());
			} while (objectListing.isTruncated());
		} catch (AmazonServiceException ase) {
			return null;
		} catch (AmazonClientException ace) {
			return null;
		}
		return s3ObjectSummaries;
	}

	public List<String> getBucketObjectNames(String url) throws Exception{
		List<String> s3ObjectNames = new ArrayList<String>();
		List<S3ObjectSummary> s3ObjectSummaries = getBucketObjectSummaries(url);

		if(s3ObjectSummaries == null || s3ObjectSummaries.size() == 0){
			s3ObjectNames.add("invalid");
			return s3ObjectNames;
		}
		for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries) {
			s3ObjectNames.add(s3ObjectSummary.getKey());
		}
		return s3ObjectNames;
	}
}
