package pro.mneura.data.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import pro.mneura.data.entity.Transcript;
import pro.mneura.data.intergration.HibernateIntegration;

import java.util.List;

public class TranscriptDao {

    public void save(Transcript transcript) {
        Session session = HibernateIntegration.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(transcript);
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

    public List<Transcript> getAll() {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            return session
                    .createQuery("from Transcript order by createdAt desc", Transcript.class)
                    .list();
        }
    }


    public List<Transcript> getFiltered(long fromEpochSeconds, long toEpochSeconds, String category) {
        try (Session session = HibernateIntegration.getSessionFactory().openSession()) {
            boolean hasCategory = category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL");

            String hql = "from Transcript t where t.createdAt between :from and :to";
            if (hasCategory) {
                hql += " and t.category = :category";
            }
            hql += " order by t.createdAt desc";

            Query<Transcript> query = session.createQuery(hql, Transcript.class)
                    .setParameter("from", fromEpochSeconds)
                    .setParameter("to", toEpochSeconds);

            if (hasCategory) {
                query.setParameter("category", category.trim().toUpperCase());
            }

            return query.list();
        }
    }
}