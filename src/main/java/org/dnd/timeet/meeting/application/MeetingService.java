package org.dnd.timeet.meeting.application;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.dnd.timeet.agenda.domain.AgendaRepository;
import org.dnd.timeet.agenda.domain.AgendaStatus;
import org.dnd.timeet.agenda.domain.AgendaType;
import org.dnd.timeet.agenda.dto.AgendaReportInfoResponse;
import org.dnd.timeet.common.exception.BadRequestError;
import org.dnd.timeet.common.exception.InternalServerError;
import org.dnd.timeet.common.exception.NotFoundError;
import org.dnd.timeet.common.utils.TimeUtils;
import org.dnd.timeet.meeting.domain.Meeting;
import org.dnd.timeet.meeting.domain.MeetingRepository;
import org.dnd.timeet.meeting.dto.MeetingCreateRequest;
import org.dnd.timeet.meeting.dto.MeetingReportInfoResponse;
import org.dnd.timeet.member.domain.Member;
import org.dnd.timeet.participant.domain.Participant;
import org.dnd.timeet.participant.domain.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 의존성 주입
@Transactional // DB 변경 작업에 사용
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final AgendaRepository agendaRepository;

    public Meeting createMeeting(MeetingCreateRequest createDto, Member member) {
        Meeting meeting = createDto.toEntity(member);
        meeting = meetingRepository.save(meeting);

        Participant participant = new Participant(meeting, member);
        participantRepository.save(participant);

        return meeting;
    }

    public Meeting addParticipantToMeeting(Long meetingId, Member member) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new NotFoundError(NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                Collections.singletonMap("MeetingId", "Meeting not found")));

        // 멤버가 이미 회의에 참가하고 있는지 확인
        boolean alreadyParticipating = meeting.getParticipants().stream()
            .anyMatch(participant -> participant.getMember().equals(member));

        if (alreadyParticipating) {
            // 에러 메세지 발생
            throw new BadRequestError(BadRequestError.ErrorCode.DUPLICATE_RESOURCE,
                Collections.singletonMap("Member", "Member already participating in the meeting"));
        }

        // Participant 인스턴스 생성 및 저장
        Participant participant = new Participant(meeting, member);
        participantRepository.save(participant);

        // 양방향 연관관계 설정
        meeting.getParticipants().add(participant);
        member.getParticipations().add(participant);

        return meeting;
    }

    public MeetingReportInfoResponse createReport(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new NotFoundError(NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                Collections.singletonMap("MeetingId", "Meeting not found")));

        List<AgendaReportInfoResponse> agendaReportInfoResponses = agendaRepository.findByMeetingId(meetingId).stream()
            .filter(agenda -> agenda.getType() == AgendaType.AGENDA && agenda.getStatus() == AgendaStatus.COMPLETED)
            .map(AgendaReportInfoResponse::from)
            .collect(Collectors.toList());

        return MeetingReportInfoResponse.builder()
            .totalDiff(
                TimeUtils.calculateTimeDiff(meeting.getTotalEstimatedDuration(), meeting.getTotalActualDuration()))
            .agendas(agendaReportInfoResponses)
            .memos("회의록입니다.")
            .build();

    }

    private LocalTime calculateTotalDiff(LocalTime totalEstimatedDuration, LocalTime totalActualDuration) {
        if (totalEstimatedDuration == null || totalActualDuration == null) {
            throw new InternalServerError(InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                Collections.singletonMap("Meeting", "Meeting totalEstimatedDuration or totalActualDuration is null"));
        }

        return totalEstimatedDuration.minusHours(totalActualDuration.getHour())
            .minusMinutes(totalActualDuration.getMinute());
    }


    @Transactional(readOnly = true)
    public Meeting findById(Long id) {
        return meetingRepository.findById(id)
            .orElseThrow(() -> new NotFoundError(NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                Collections.singletonMap("MeetingId", "Meeting not found")));
    }

    public void removeParticipant(Long meetingId, Member member) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new NotFoundError(NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                Collections.singletonMap("MeetingId", "Meeting not found")));

        if (meeting.getHostMember().equals(member)) {
            meeting.assignNewHostRandomly();
        }

    }

}
