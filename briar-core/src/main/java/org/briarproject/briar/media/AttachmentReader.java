package org.briarproject.briar.media;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.client.ClientHelper;
import org.briarproject.bramble.api.data.BdfDictionary;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.api.media.Attachment;
import org.briarproject.briar.api.media.AttachmentHeader;
import org.briarproject.briar.api.media.InvalidAttachmentException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.briarproject.briar.media.MediaConstants.MSG_KEY_CONTENT_TYPE;
import static org.briarproject.briar.media.MediaConstants.MSG_KEY_DESCRIPTOR_LENGTH;

public class AttachmentReader {

	public static Attachment getAttachment(ClientHelper clientHelper,
			AttachmentHeader h) throws DbException {
		// TODO: Support large messages
		MessageId m = h.getMessageId();
		byte[] body = clientHelper.getMessage(m).getBody();
		try {
			BdfDictionary meta = clientHelper.getMessageMetadataAsDictionary(m);
			String contentType = meta.getString(MSG_KEY_CONTENT_TYPE);
			if (!contentType.equals(h.getContentType()))
				throw new InvalidAttachmentException();
			int offset;
			try {
				offset = meta.getLong(MSG_KEY_DESCRIPTOR_LENGTH).intValue();
			} catch (FormatException e) {
				throw new InvalidAttachmentException();
			}
			InputStream stream = new ByteArrayInputStream(body, offset,
					body.length - offset);
			return new Attachment(h, stream);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

}
