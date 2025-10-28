package edu.ucsb.cs156.example.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** The UCSBDateRepository is a repository for RecommendationRequest entities. */
@Repository
public interface RecommendationRequestRepository extends CrudRepository<UCSBDate, Long> {

  Iterable<UCSBDate> findAllByQuarterYYYYQ(String quarterYYYYQ);
}
