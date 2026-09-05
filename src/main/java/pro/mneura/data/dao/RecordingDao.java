package pro.mneura.data.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import pro.mneura.data.entity.Recording;
import pro.mneura.status.RecordingStatus;
import pro.mneura.data.intergration.HibernateIntegration;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecordingDao {

    public Set<String> getAllRecordingNames() {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            List<String> names = session
                    .createQuery("select r.recordingName from Recording r", String.class)
                    .list();
            return new HashSet<>(names);
        }
    }

    public List<Recording> getAll() {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            return session
                    .createQuery("from Recording order by createdAt desc", Recording.class)
                    .list();
        }
    }

    public List<Recording> getUntranscribed() {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            return session
                    .createQuery("from Recording r where r.isTranscripted = false order by r.createdAt asc", Recording.class)
                    .list();
        }
    }

    public List<Recording> getFiltered(long fromEpochSeconds, long toEpochSeconds) {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            return session
                    .createQuery(
                            "from Recording r where r.createdAt between :from and :to order by r.createdAt desc",
                            Recording.class)
                    .setParameter("from", fromEpochSeconds)
                    .setParameter("to", toEpochSeconds)
                    .list();
        }
    }

    public void saveNewRecording(String fileName) {
        Session session = HibernateIntegration.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Recording recording = new Recording(fileName);
            recording.setStatus(RecordingStatus.CACHED);
            recording.setTranscripted(false);
            session.persist(recording);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public void markTranscribed(String recordingName) {
        Session session = HibernateIntegration.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.createMutationQuery(
                            "update Recording r set r.isTranscripted = true, r.updatedAt = :now where r.recordingName = :name")
                    .setParameter("now", Instant.now().getEpochSecond())
                    .setParameter("name", recordingName)
                    .executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }
}