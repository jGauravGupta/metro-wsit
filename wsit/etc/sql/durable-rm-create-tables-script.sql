DROP TABLE RM_UNACKED_MESSAGES;
DROP TABLE RM_SEQUENCES;
DROP TABLE RM_LOCALIDS;

CREATE TABLE RM_SEQUENCES (
ENDPOINT_UID VARCHAR(512) NOT NULL,
ID VARCHAR(256) NOT NULL,
TYPE CHARACTER NOT NULL,

EXP_TIME BIGINT NOT NULL,
BOUND_ID VARCHAR(256),
STR_ID VARCHAR(256),

STATUS SMALLINT NOT NULL,
ACK_REQUESTED_FLAG CHARACTER,
LAST_MESSAGE_NUMBER BIGINT NOT NULL,
LAST_ACTIVITY_TIME BIGINT NOT NULL,
LAST_ACK_REQUEST_TIME BIGINT NOT NULL,

PRIMARY KEY (ENDPOINT_UID, ID)
);

CREATE INDEX IDX_RM_SEQUENCES_BOUND_ID ON RM_SEQUENCES (BOUND_ID);

CREATE TABLE RM_UNACKED_MESSAGES (
ENDPOINT_UID VARCHAR(512) NOT NULL,
SEQ_ID VARCHAR(256) NOT NULL,
MSG_NUMBER BIGINT NOT NULL,
IS_RECEIVED CHARACTER NOT NULL,

CORRELATION_ID VARCHAR(256),
NEXT_RESEND_COUNT INT,
WSA_ACTION VARCHAR(256),
MSG_DATA BLOB,

PRIMARY KEY (ENDPOINT_UID, SEQ_ID, MSG_NUMBER)
);

ALTER TABLE RM_UNACKED_MESSAGES
ADD CONSTRAINT FK_SEQUENCE
FOREIGN KEY (ENDPOINT_UID, SEQ_ID) REFERENCES RM_SEQUENCES(ENDPOINT_UID, ID);

CREATE INDEX IDX_RM_UNACKED_MESSAGES_CORRELATION_ID ON RM_UNACKED_MESSAGES (CORRELATION_ID);

CREATE TABLE RM_LOCALIDS (
LOCAL_ID VARCHAR(512) NOT NULL,
SEQ_ID VARCHAR(256) NOT NULL,
MSG_NUMBER BIGINT NOT NULL,
CREATE_TIME BIGINT,
SEQ_TERMINATE_TIME BIGINT,

PRIMARY KEY (LOCAL_ID)
);
