package edu.ucsb.cs156.example.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** The RecommendationRequestRepository is a repository for RecommendationRequest entities. */
@Repository
public interface RecommendationRequestRepository
    extends CrudRepository<RecommendationRequest, Long> {}
