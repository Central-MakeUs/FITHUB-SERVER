package fithub.app.service;

import fithub.app.domain.Article;
import fithub.app.domain.Record;
import fithub.app.domain.User;
import fithub.app.web.dto.requestDto.RecordRequestDto;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.List;

public interface RecordService {

    Record create(RecordRequestDto.CreateRecordDto request, User user, Integer categoryId) throws IOException;

    Record getRecord(Long recordId);

    Boolean getIsLiked(Record record, User user);

    Record toggleRecordLike(Long recordId, User user);

    Record updateRecord(RecordRequestDto.updateRecordDto request, Long recordId, User user) throws IOException;

    void deleteRecordSingle(Long recordId, User user);

    Page<Record> findRecordPagingCategoryAndCreatedAt(User user, Integer categoryId, Integer pageIndex);
    Page<Record> findRecordPagingCreatedAt(User user, Integer pageIndex);

    Page<Record> findRecordPagingCategoryAndLikes(User user, Integer categoryId, Long last);
    Page<Record> findRecordPagingLikes(User user, Long last);

    void calcExp(User user, Integer categoryId);

    void deleteRecordBulk(RecordRequestDto.deleteListRecordDto request, User user);
}
